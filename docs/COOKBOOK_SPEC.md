# Mobdysseus — On-device Cookbook Spec (faithful port of the desktop Cookbook)

> **Goal:** Replicate the desktop Odysseus **Cookbook** — the model lifecycle manager — on the Galaxy S25, matching its functionality and UX as closely as the platform allows.
>
> **Fidelity principle:** every desktop Cookbook capability is either (a) ported on-device, (b) proxied to a remote Odysseus server, or (c) explicitly declared not-applicable with a reason. Nothing is silently dropped.

---

## 1. What the desktop Cookbook actually does (the thing we replicate)

The Cookbook is a **full model lifecycle manager**, not just a "recommend a model" feature. Its complete capability set (verified from `routes/cookbook_routes.py`, `routes/cookbook_helpers.py`, `services/hwfit/*`, `src/cookbook_serve_lifecycle.py`, and the `static/js/cookbook*.js` frontend):

| # | Capability | Desktop implementation |
|---|---|---|
| 1 | **Hardware detection** | `services/hwfit/hardware.py` — probes NVIDIA (`nvidia-smi`), AMD, Apple Silicon, CPU/RAM/VRAM, incl. remote SSH rigs |
| 2 | **Model registry + quant math** | `services/hwfit/models.py` — `QUANT_BPP`, `QUANT_SPEED_MULT`, `QUANT_QUALITY_PENALTY`, `QUANT_BYTES_PER_PARAM`, memory estimation |
| 3 | **Fit-scoring engine** | `services/hwfit/fit.py` — `rank_models()`: speed estimate, architecture bonus, quality score, RAM/VRAM fit, context score |
| 4 | **Serve profiles** | `services/hwfit/profiles.py` — Quality/Balanced/Speed llama.cpp flags (n_gpu_layers, cache-type, context) |
| 5 | **Latest-model discovery** | `services/hwfit/hf_discovery.py` + `/api/cookbook/hf-latest` — HuggingFace `api/models?sort=trendingScore` filtered by VRAM fit, quant, pipeline; excludes LoRAs/adapters/datasets/embeddings |
| 6 | **GGUF file listing** | `/api/cookbook/hf-gguf-files` — list GGUF files in a repo |
| 7 | **Ollama library** | `/api/cookbook/ollama/library` |
| 8 | **Model download** | `/api/model/download` + `/api/model/cached` — HF/Ollama download, cache scan, progress |
| 9 | **Model serving** | `/api/model/serve` — vLLM/SGLang/llama.cpp/MLX/Ollama/LM Studio/lmdeploy/koboldcpp/exllama under tmux |
| 10 | **Serve lifecycle** | `src/cookbook_serve_lifecycle.py` — background loop, phase parsing (loading/downloading/warming-up/ready/tok/s) |
| 11 | **Dependency setup** | `/api/cookbook/setup` — pip/venv install, llama.cpp rebuild, dependency recipes |
| 12 | **Dependency diagnosis** | `cookbook-diagnosis.js` — parse serve output, suggest fixes |
| 13 | **GPU listing** | `/api/cookbook/gpus` |
| 14 | **Process control** | `/api/cookbook/kill-pid`, `/api/cookbook/state` (GET/POST) |
| 15 | **Remote rigs (SSH)** | `/api/cookbook/ssh-key`, `/api/cookbook/test-ssh` |
| 16 | **vLLM recipes** | `/api/cookbook/vllm-recipe-manifest`, `/api/cookbook/vllm-recipe` |
| 17 | **Task status** | `/api/cookbook/tasks/status` — background download/serve progress |
| 18 | **Scheduling** | `cookbookSchedule.js` — scheduled serve jobs |
| 19 | **Progress signal** | `cookbookProgressSignal.js` — aggregate progress computation |
| 20 | **Running-job cards** | `cookbookRunning.js` — live job monitoring UI |

---

## 2. Dual-mode architecture

The Android Cookbook runs in **two modes**, which together cover the full desktop feature set:

- **On-device mode** — a local Cookbook that detects the S25, discovers/ranks/downloads **GGUF** models, and serves them via **llama.cpp** (the only on-device backend). Covers capabilities 1–10, 14, 17–20 locally.
- **Remote mode** — the app drives the **desktop Cookbook** running on a home server/Pi/cloud by proxying `/api/cookbook/*` and `/api/model/*`. This gives the app *the exact desktop Cookbook* (vLLM/SGLang/MLX, GPU detection, SSH rigs, vLLM recipes, dependency setup) with zero reimplementation — the app is a native UI over the same API.

This is the same pattern as the rest of the rebuild (hybrid client-server): **on-device for what a phone can do, remote proxy for what only a server can do.**

---

## 3. Component mapping (desktop → Android)

| Desktop capability | On-device | Remote proxy | Not applicable | Notes |
|---|---|---|---|---|
| Hardware detection | ✅ Kotlin (Build/ActivityManager) | ✅ (server hwfit) | — | On-device is trivial: fixed SoC/RAM/NPU |
| Model registry + quant math | ✅ Kotlin port | ✅ | — | Pure deterministic tables → port 1:1 |
| Fit-scoring engine | ✅ Kotlin port | ✅ | — | `rank_models()` is pure math |
| Serve profiles | ✅ llama.cpp config | ✅ (vLLM/SGLang/MLX) | — | On-device: threads/context/mmap/NPU |
| Latest-model discovery | ✅ HF API (GGUF filter) | ✅ | — | Same `api/models` endpoint |
| GGUF file listing | ✅ | ✅ | — | |
| Ollama library | — | ✅ | ❌ on-device | Ollama doesn't run on Android |
| Model download | ✅ GGUF downloader | ✅ | — | |
| Model serving | ✅ llama.cpp | ✅ (all backends) | — | On-device: llama.cpp only |
| Serve lifecycle | ✅ FGS + state machine | ✅ | — | |
| Dependency setup | ⚠️ → "runtime readiness" | ✅ | — | No pip/venv on Android; replaced by bundled-runtime + model-download readiness |
| Dependency diagnosis | ⚠️ → readiness diagnosis | ✅ | — | |
| GPU listing | — | ✅ | ❌ on-device | No discrete GPU; NPU is the analog |
| Process control | ✅ (in-process) | ✅ | — | |
| Remote rigs (SSH) | — | ✅ | ❌ on-device | Server-side concept |
| vLLM recipes | — | ✅ | ❌ on-device | vLLM doesn't run on Android |
| Task status | ✅ | ✅ | — | |
| Scheduling | ✅ WorkManager | ✅ | — | |
| Progress signal | ✅ | ✅ | — | |
| Running-job cards | ✅ | ✅ | — | |

**Net:** ~14 of 20 capabilities are fully on-device; the 6 server-only ones (Ollama, GPU listing, SSH rigs, vLLM recipes, dependency setup/diagnosis) are delivered via remote mode. **Nothing is lost.**

---

## 4. On-device Cookbook — detailed design

### 4.1 Hardware detection (Kotlin)

```kotlin
data class DeviceHardware(
    val socModel: String,        // Build.SOC_MODEL → "Snapdragon 8 Elite"
    val cpuCores: Int,           // Runtime.availableProcessors()
    val totalRamGb: Float,       // ActivityManager.MemoryInfo.totalMem
    val usableRamGb: Float,      // totalRam - OS/OneUI overhead (~3-4GB)
    val hasNpu: Boolean,         // QNN/ExecuTorch availability probe
    val npuTops: Float?,         // ~45 TOPS if Hexagon present
    val freeStorageGb: Float,    // StatFs
    val thermalState: Int,       // PowerManager.THERMAL_STATUS_*
)
```

No probing, no subprocess — instant and deterministic. This is the *input* to the scoring engine.

### 4.2 Model discovery (HuggingFace contract)

Reuse the desktop's exact query shape, filtered for GGUF:

```
GET https://huggingface.co/api/models?sort=trendingScore&direction=-1&limit={pool}&filter=text-generation
```

Then apply the desktop's own filters (ported verbatim):
- **VRAM→RAM fit**: estimate params from repo id (`7B`, `1.5B`…), × quant factor (fp4/nf4/int4/q4 → 0.25, int8/q8 → 0.5, bf16/fp16 → 1.0).
- **Exclude** LoRAs/adapters/peft/qlora/datasets/embeddings/merges (same `EXCLUDE_TAG_SUBSTRINGS` / `EXCLUDE_NAME_SUBSTRINGS` lists).
- **GGUF-only filter** (on-device addition): keep repos with GGUF files (via `/api/cookbook/hf-gguf-files` equivalent), drop safetensors-only repos.

Plus a **curated catalog** (managed list, updated weekly) as the primary "latest & best for S25" source, with live HF search as the "browse latest" power-user path. This addresses the upstream ROADMAP's known weakness (raw HF ranking scores everything ~the same).

### 4.3 Fit-scoring engine (Kotlin port of `fit.py` + `models.py`)

Port these **deterministic** functions 1:1 (they are pure math, no I/O):

| Upstream | Kotlin equivalent |
|---|---|
| `QUANT_BPP` / `QUANT_BYTES_PER_PARAM` | `QuantTable.bpp(quant)`, `bytesPerParam(quant)` |
| `QUANT_SPEED_MULT` | `QuantTable.speedMult(quant)` |
| `QUANT_QUALITY_PENALTY` | `QuantTable.qualityPenalty(quant)` |
| `estimate_memory_gb()` | `MemoryEstimator.estimate(model, quant, ctx)` |
| `_estimate_speed()` | `SpeedEstimator.estimate(model, quant, device)` |
| `_architecture_bonus()` | `architectureBonus(model)` |
| `_quality_score()` | `qualityScore(model, quant, useCase)` |
| `_fit_score()` | `fitScore(requiredGb, availableGb)` |
| `_context_score()` | `contextScore(ctx, useCase)` |
| `rank_models()` | `ModelRanker.rank(device, useCase, limit, search, sort, quant, ctx)` |

**Key adaptation:** replace GPU bandwidth/VRAM with **RAM + NPU + thermal**:
- `availableRamGb` (≈8–10 GB) instead of `gpu_vram`.
- Speed estimate uses **CPU (ARM NEON) + Adreno 830 (Vulkan) + Hexagon NPU** tiers instead of GPU bandwidth table.
- Add a **thermal throttle factor** (S25 sustained inference throttles; penalize 7B+ models).

**Default recommendation** (from the S25 research): **3B–4B Q4_K_M** as the sweet spot (10–25 tok/s, fits RAM); **7B–8B Q4** as "high quality" (5–12 tok/s, thermal risk); **1B–2B** as "fast/always-on."

### 4.4 Serve profiles (llama.cpp config mapping)

Port `profiles.py`'s Quality/Balanced/Speed concept to on-device llama.cpp flags:

| Profile | Context | Quant | Threads | mmap | NPU offload |
|---|---|---|---|---|---|
| **Quality** | 8192 | Q8_0/Q6_K | all cores | yes | no |
| **Balanced** | 4096 | Q4_K_M | all cores | yes | optional |
| **Speed** | 2048 | Q4_0/Q3_K_M | all cores | yes | yes (if QNN) |

Generated config feeds the llama.cpp JNI runtime directly (no tmux, no subprocess — in-process or a bound foreground service).

### 4.5 Download manager

- GGUF download via OkHttp with **resumable** range requests + progress (mirrors `cookbookDownload.js`).
- Store weights in **app-private storage** (or Play Asset Delivery for the bundled default model).
- Cache scan (`/api/model/cached` equivalent) → list downloaded models + sizes.
- On-demand model selection (user picks 3B vs 7B) — avoids Play's 100 MB APK limit.

### 4.6 Serve lifecycle (foreground service)

Port `cookbook_serve_lifecycle.py`'s state machine to a **foreground service** (`specialUse` type):

```
idle → loading (pct) → warming-up → ready (tok/s) → serving → idle/unload
```

- **On-demand load**: load model → serve → idle/unload (not a permanent resident server) — critical for battery + Samsung's memory killer.
- **Graceful restart**: persist state; resume on reconnect/kill.
- Phase parsing is *simpler* on-device (no tmux snapshot regex) — the llama.cpp runtime reports progress directly via a callback.

### 4.7 Running jobs / progress / scheduling

- **Running-job cards** (`cookbookRunning.js`): live cards for download/serve jobs with phase, %, tok/s.
- **Progress signal** (`cookbookProgressSignal.js`): aggregate progress across jobs.
- **Scheduling** (`cookbookSchedule.js`): WorkManager for scheduled model warm-up/serve (e.g., "preload 3B at 7am").

### 4.8 Diagnosis (readiness check)

Replaces the desktop's pip/venv diagnosis with a **runtime readiness check**:
- llama.cpp `.so` present + ABI correct (arm64-v8a)?
- Model file present + checksum valid?
- Enough free RAM/storage?
- NPU/QNN available?
- Thermal state OK?
Each check → a clear, actionable fix (download runtime, free space, close apps, cool down).

---

## 5. Remote Cookbook proxy (drives the desktop Cookbook)

The app exposes the **same Cookbook UI** in "remote" mode, proxying the desktop endpoints over the authenticated companion bridge:

| Desktop endpoint | App action |
|---|---|
| `GET /api/cookbook/gpus` | Show server GPUs |
| `GET /api/cookbook/hf-latest` | Latest models (server VRAM fit) |
| `POST /api/model/download` | Download on server |
| `POST /api/model/serve` | Serve on server (vLLM/SGLang/MLX/…) |
| `GET /api/cookbook/tasks/status` | Job progress |
| `POST /api/cookbook/setup` | Install deps on server |
| `GET /api/cookbook/vllm-recipe-manifest` | vLLM recipes |
| `POST /api/cookbook/ssh-key` / `test-ssh` | Manage remote rigs |
| `POST /api/cookbook/kill-pid` | Stop a job |

This is a thin native UI over the existing API — **zero server changes**, 100% desktop Cookbook fidelity.

---

## 6. Data model (Room)

```kotlin
@Entity table "models"        // downloaded + discovered models
  id, repoId, name, paramsB, quant, sizeGb, license, visionSupport,
  architecture, source (hf/curated), downloadedAt, path

@Entity table "serve_jobs"    // running/scheduled serve jobs
  id, modelId, profile (quality/balanced/speed), context, status,
  phase, pct, tokPerSec, startedAt, mode (local/remote)

@Entity table "download_jobs" // download progress
  id, modelId, url, bytesTotal, bytesDone, status, resumeToken

@Entity table "device_profile" // cached hardware detection
  socModel, ramGb, hasNpu, npuTops, freeStorageGb, detectedAt
```

---

## 7. API contract (local, in-app)

The on-device Cookbook exposes a **local** API mirroring the desktop shape (so the WebView bridge and native UI share one contract):

```
GET  /local/cookbook/hardware          → DeviceHardware
GET  /local/cookbook/models?useCase=   → ranked models (ModelRanker)
GET  /local/cookbook/hf-latest?limit=  → HF discovery (GGUF-filtered)
POST /local/model/download             → start GGUF download
GET  /local/model/cached               → downloaded models
POST /local/model/serve                → start llama.cpp serve (profile)
GET  /local/cookbook/tasks/status      → job progress
POST /local/cookbook/kill              → stop a job
GET  /local/cookbook/readiness         → diagnosis checks
```

---

## 8. Agent-swarm task breakdown (for the new chat)

| Agent | Task | Depends on |
|---|---|---|
| **Cookbook-Scoring Agent** | Port `models.py` + `fit.py` + `profiles.py` to Kotlin (quant tables, memory/speed/quality/fit/context scoring, rank_models, serve profiles) with unit tests against known desktop outputs | — |
| **Cookbook-Hardware Agent** | Implement `DeviceHardware` detection (Build/ActivityManager/StatFs/NPU probe/thermal) | — |
| **Cookbook-Discovery Agent** | HF `api/models` client + GGUF filter + curated catalog + `hf-gguf-files` | — |
| **Cookbook-Download Agent** | Resumable GGUF downloader + cache scan + progress | Scaffold |
| **Cookbook-Serve Agent** | llama.cpp JNI runtime + foreground service + lifecycle state machine + serve profiles | Scoring, Download |
| **Cookbook-UI Agent** | Compose UI: hardware panel, ranked model list, download/serve cards, progress, scheduling, diagnosis | Scoring, Serve |
| **Cookbook-Remote Agent** | Remote proxy of `/api/cookbook/*` + `/api/model/*` over the companion bridge | API Contract Agent |
| **Cookbook-QA Agent** | Verify: ranking matches desktop for a known rig, download resumes, serve reaches "ready" with real tok/s, thermal behavior | all |

**Sequencing:** Scoring + Hardware + Discovery in parallel → Download + Serve → UI + Remote → QA.

---

## 9. Acceptance criteria (Cookbook)

- [ ] Hardware panel shows correct S25 specs (SoC, RAM, NPU, storage, thermal) instantly.
- [ ] "Latest models" list is GGUF-only, ranked by fit, with the 3B–4B Q4 sweet spot surfaced first.
- [ ] Ranking for a *known* hardware profile matches the desktop `rank_models()` output (parity test).
- [ ] Download is resumable, shows live %, and survives app backgrounding.
- [ ] Serve reaches "ready" and reports real tok/s; Quality/Balanced/Speed profiles produce the expected context/quant/threads.
- [ ] On-demand load/unload keeps idle RAM <300 MB and active battery <5%/hr.
- [ ] Remote mode drives the desktop Cookbook (download/serve/recipes/SSH rigs) with no server changes.
- [ ] Diagnosis gives actionable fixes for missing runtime/model/RAM/thermal.

---

*This spec is grounded in the upstream implementation (`services/hwfit/*`, `routes/cookbook_routes.py`, `routes/cookbook_helpers.py`, `src/cookbook_serve_lifecycle.py`, `static/js/cookbook*.js`). It is a companion to `REBUILD_PLAN.md` and slots into **Phase 2** of that plan.*
