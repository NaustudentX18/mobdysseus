# Mobdysseus — Rebuild Plan (Audit & Plan Phase Deliverable)

> **Target:** Rebuild [`odysseus-dev/odysseus`](https://github.com/odysseus-dev/odysseus) — a self-hosted AI workspace — as a native Android APK **exactly designed and optimised for the Samsung Galaxy S25**.
>
> **Status:** This is the *audit + plan* phase output. It is self-contained: an agent swarm in a **new chat** can read this file and execute it without re-doing the research.
>
> **Upstream reference clone:** `/root/mobdysseus` (the AGPL-3.0 upstream repo, used for research).

---

## 0. Executive Summary

**What Odysseus actually is** (verified, not assumed): a **self-hosted AI workspace** — chat + agents, deep research, documents, email, notes/tasks/calendar, gallery, MCP, and local-model workflows. It is **not** an iOS jailbreak and **not** a mobile app. It is a **Python/FastAPI server** (~244K lines Python, 175 JS files, ~488 HTTP endpoints) with a no-build vanilla-JS web client, deployed via Docker.

**The single most important decision:** Odysseus is a *server*. A "native Android app" is therefore a **native client** to that server, with a subset of features runnable on-device. The agent loop, tool execution, model routing, RAG, and MCP layer (~1 MB of tuned Python) *are* the product and are coupled to a Python/GPU server. Reimplementing them on-device is 2–4 person-years with guaranteed fidelity drift; running Python on Android is blocked (torch/transformers/vLLM don't build on Android).

**Chosen architecture (decisive):** **Hybrid client-server** — a native **Kotlin + Jetpack Compose** client that talks to the real Odysseus server (100% fidelity by construction), **bootstrapped by a WebView wrapper** as a 2-week day-1 bridge, plus a **thin on-device model layer** (llama.cpp / MediaPipe) for offline features.

**Effort:** ~20–24 weeks (1–2 engineers) to a polished v1; ~26–40 engineer-weeks for full parity.

**License:** AGPL-3.0-or-later. Not an obstacle, but the APK must ship full buildable source, stay AGPL, and be **rebranded** (do not reuse the "Odysseus" name/wordmark).

---

## 1. Target Understanding (verified facts)

| Dimension | Fact |
|---|---|
| Framework | FastAPI + uvicorn, port 7000 (`APP_BIND`/`APP_PORT`), single-process, async |
| Streaming | SSE (`res.body.getReader()`), **no WebSocket**; 45s hard-request-timeout middleware with a streaming whitelist |
| API surface | **488 endpoints** across ~50 route modules |
| Auth | Cookie sessions (bcrypt, `token_hex(32)`, 7-day TTL, `data/sessions.json`) + scoped `ody_` bearer API tokens + TOTP 2FA + `LOCALHOST_BYPASS` + internal-tool loopback token; `require_admin` gates 22 modules |
| Data model | SQLite `data/app.db` (SQLAlchemy, ~25 tables) + JSON files under `data/` + ChromaDB (HTTP client, port 8100) + fastembed ONNX embeddings; Fernet encryption-at-rest for secrets |
| Frontend | Vanilla ES6 modules, **no build step, no framework**; 175 JS files (~117K lines); `static/app.js` orchestrator; module map in `static/js/MODULE_SUMMARY.md` |
| MCP | 4 built-in stdio MCP servers (`mcp_servers/`), managed by `src/mcp_manager.py` + `src/builtin_mcp.py`; MCP SDK v1 |
| Integrations | `integrations/claude` + `integrations/codex` (skill bundles → scoped `/api/codex/*`); `companion/` = existing thin-client pairing seam |
| Model serving | Cookbook downloads via HF/Ollama, serves via vLLM/SGLang/llama.cpp/MLX/Ollama/LM Studio/lmdeploy/koboldcpp/exllama under tmux; `services/hwfit/` detects NVIDIA/AMD/Apple Silicon |
| Swift bridge | macOS-only MLX image bridge (inpaint LaMa/MIGAN, colorize DDColor), `.macOS(.v26)` — no Android relevance except as a pattern for shelling out to native models |
| License | AGPL-3.0-or-later; core deliberately MIT-compatible; PyMuPDF (AGPL) is the one optional copyleft flag |

**Key file-path index (upstream):** `app.py` (orchestrator), `core/models.py` (SQLAlchemy), `core/auth.py`, `core/database.py`, `src/llm_core.py` (LLM client), `src/agent_loop.py`, `src/tool_schemas.py`, `src/mcp_manager.py`, `src/caldav_sync.py`, `src/task_scheduler.py`, `routes/*_routes.py` (50 modules), `static/js/*` (frontend), `companion/` (pairing bridge).

---

## 2. The Core Architectural Decision

### 2.1 Candidate comparison

| # | Architecture | Effort | Fidelity | On-device LLM | Offline | S25 optimization | Verdict |
|---|---|---|---|---|---|---|---|
| A | Native Kotlin/Compose full reimplementation | 2–4 person-years | Drift risk | Yes | Yes | Best | ❌ Too costly |
| B | WebView wrapper (embed server + render existing frontend) | 2 weeks | 100% UI | Partial | Partial | Poor | ✅ **Day-1 bridge only** |
| C | **Hybrid client-server** (native client → real server) | 20–24 wks | 100% by construction | Yes (thin layer) | Partial | Excellent | ✅ **TARGET** |
| D | Flutter / React Native | ~1 year | Drift risk | Partial | Partial | Good | ❌ No advantage over C |
| E | On-device Python (Chaquopy/BeeWare/Termux) | N/A | N/A | N/A | N/A | N/A | ❌ Blocked (torch/vLLM don't build) |

### 2.2 Decision

**Architecture C (Hybrid client-server), bootstrapped by B (WebView wrapper).**

- The **real Odysseus server** (unmodified upstream, or lightly patched) runs on a home server / Raspberry Pi / cloud. The Android app is a **polished native client**.
- The existing **`/api/companion/*` pairing bridge** (`ping|info|models|pair`) is the purpose-built thin-client seam — the Android client authenticates through it.
- A **thin on-device layer** provides offline notes/tasks/calendar/email cache + a local small LLM (3B–4B Q4 GGUF) for offline chat/summarization.
- **Day 1:** ship a WebView wrapper rendering the existing frontend against the server (instant 100% UI fidelity) while the native client is built in parallel.

### 2.3 Concrete tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.x |
| UI | Jetpack Compose (Material 3, adaptive layouts) |
| Networking | Ktor client + OkHttp; **SSE** for streaming (matches server), WebSocket optional |
| Local server (optional) | Ktor embedded server on `127.0.0.1` for the WebView bridge |
| Auth | Device-flow OAuth (already in repo) + session/token persistence |
| Storage | Room (SQLite) + DataStore (prefs) + Android Keystore (secrets) |
| On-device LLM | **llama.cpp** (GGUF, JNI) primary; **MediaPipe LLM Inference** (Gemma 2B/3B) secondary; **ExecuTorch/QNN** for NPU later |
| Background | WorkManager (periodic sync) + foreground service (`specialUse` type) + partial wakelock |
| Connectivity | Direct HTTPS, or Tailscale/WireGuard for secure remote access |
| S25 polish | DeX adaptive layouts, One UI edge panels, S Pen, Material You dynamic color, home-screen widgets |

---

## 3. Galaxy S25 Platform Profile (constraints that shape the build)

| Item | Value | Implication |
|---|---|---|
| SoC | Snapdragon 8 Elite "for Galaxy" (Oryon 2×4.47GHz + 6×3.53GHz) | Fast CPU; on-device inference viable |
| GPU | Adreno 830 | Vulkan/OpenCL accel for llama.cpp/MLC |
| NPU | Hexagon (~45 TOPS) | 2–4× throughput via QNN, but engineering-heavy |
| RAM | 12 GB LPDDR5X (~8–10 GB usable) | **3B–4B Q4 GGUF is the sweet spot** (10–25 tok/s); 7B–8B Q4 is the ceiling (5–12 tok/s, thermal risk) |
| OS | Android 15 + One UI 7 | FGS `foregroundServiceType` required; Samsung memory killer is aggressive |
| Storage | UFS 4.0, no microSD | Model weights via Play Asset Delivery or on-demand download |

**On-device LLM guidance:** default **3B–4B Q4** (interactive, fits RAM); optional **7B–8B Q4** "high quality" download. **Gemini Nano** (ML Kit GenAI API, opened to third parties May 2025) is a *secondary* integration only — S25 device support is not guaranteed and you can't bring your own weights.

**Staying alive (hardest part):** foreground service (`specialUse` type, Play-declared) + partial wakelock + **graceful restart** (persist state, resume on reconnect). Design for **on-demand model load** (load → serve → idle/unload), not a permanent resident server. Samsung's One UI memory manager can kill background apps beyond stock Android — plan for it.

**Storage:** everything in **app-private storage**; SAF for user import/export; **do not** pursue `MANAGE_EXTERNAL_STORAGE` (Play will reject).

---

## 4. Feature Parity (condensed — ~50 features → 5 phases)

Full 50-row matrix is summarized here by bucket. Feasibility legend: **On-device / Remote-server / Hybrid / Not-feasible**.

| Bucket | Key features | Feasibility | Effort |
|---|---|---|---|
| Chat + Agents | chat, streaming, tools, MCP, files, shell, skills, memory, group chat | Remote-server (Hybrid for offline chat) | L |
| Cookbook | model scan/download/serve, hardware fit | Remote-server (server does the heavy lifting) | M (client) |
| Deep Research | multi-step web research, report gen | Remote-server | M (client) |
| Compare | blind side-by-side model testing | Remote-server | S |
| Documents | editor, AI edits, Markdown/HTML/CSV, syntax highlighting | Hybrid (offline editor + server AI) | L |
| Email | IMAP/SMTP, triage, tags, summaries, reminders, drafts | Hybrid (server sync + offline cache) | XL |
| Notes/Tasks/Calendar | reminders, todos, scheduled agents, CalDAV | Hybrid (offline-first + sync) | L |
| Gallery/Image | gallery, image editor, inpaint/colorize | Remote-server (image models are server-side) | M |
| RAG / Memory | personal docs + ChromaDB, semantic memory | Remote-server | M |
| STT / TTS | faster-whisper / Kokoro | Hybrid (on-device STT/TTS possible) | M |
| MCP | stdio + Streamable HTTP + OAuth + Playwright | Remote-server | M |
| Vault / Contacts | Vaultwarden, CardDAV | Hybrid | M |
| Extras | themes, uploads, web search, presets, sessions, 2FA, webhooks, API tokens, backup/restore | Mixed | S–M |

**Top 3 risks:** (1) chat streaming fidelity (SSE), (2) on-device NPU inference, (3) email background sync.

---

## 5. Phased Build Plan (the executable roadmap)

### Phase 0 — Foundation & Day-1 Bridge (weeks 0–2) · Effort M
- [ ] Set up Android project: Gradle, Kotlin 2.x, Compose, Ktor, Room, DataStore, Keystore.
- [ ] **WebView wrapper**: load the existing `static/` frontend (bundled in `assets/` or served by a Ktor local server) pointed at a configured Odysseus server URL.
- [ ] Server connection config + health check (`/api/companion/ping`).
- [ ] Device-flow OAuth pairing against `/api/companion/pair`; persist session/token in Keystore.
- [ ] **Deliverable:** a working APK that is a faithful mobile shell of the full Odysseus UI.

### Phase 1 — Native Shell + Core Features (weeks 2–8) · Effort L
- [ ] Native Compose app shell (nav, sidebar, theming, Material You).
- [ ] **Chat** with SSE streaming (token-by-token), markdown rendering, model picker, sessions.
- [ ] **Notes + Tasks** (offline-first Room store, sync to server).
- [ ] **Documents** editor (offline) + server AI edits.
- [ ] **Calendar** (local + CalDAV sync via server).
- [ ] Auth hardening: 2FA, token refresh, admin gating.
- [ ] **Deliverable:** native MVP replacing the WebView for core workflows.

### Phase 2 — On-device Model + Cookbook + Research (weeks 8–16) · Effort XL
- [ ] Integrate **llama.cpp** (JNI) + GGUF download manager (Play Asset Delivery or on-demand).
- [ ] Offline chat/summarization with 3B–4B Q4 model; optional 7B–8B.
- [ ] **MediaPipe LLM Inference** (Gemma 2B/3B) as a secondary path.
- [ ] **Full on-device Cookbook** — faithful port of the desktop model-lifecycle manager (hardware detection, HF discovery, fit-scoring, download, serve, scheduling, diagnosis) + a remote proxy of the desktop Cookbook. Detailed spec: **`COOKBOOK_SPEC.md`**.
- [ ] **Deep Research** client (start/monitor/report view).
- [ ] Foreground service + graceful restart for local inference.
- [ ] **Deliverable:** offline-capable chat + research, on-device model running.

### Phase 3 — Email + Calendar + Gallery + MCP + Agents (weeks 16–24) · Effort XL
- [ ] **Email** (IMAP/SMTP via server, offline cache, notifications, triage).
- [ ] **Gallery** + image editor (server-side image models).
- [ ] **MCP** management UI (list/configure servers).
- [ ] **Agents** (run/monitor agent loops on the server).
- [ ] RAG / memory surfaces; STT/TTS (on-device where possible).
- [ ] Vault + Contacts (CardDAV).
- [ ] **Deliverable:** near-full feature parity.

### Phase 4 — S25 Polish + Hardening (weeks 24+) · Effort L–M
- [ ] **DeX** adaptive layouts (resizable, multi-window, keyboard/mouse).
- [ ] **One UI** integration: edge panels, S Pen, widgets, dynamic color.
- [ ] **NPU acceleration** (ExecuTorch/QNN) for supported models.
- [ ] Battery optimization (<5%/hr active), cold start <1s, idle <300MB.
- [ ] Accessibility, empty states, error messages.
- [ ] Compliance pass (see §7) + Play Store submission.
- [ ] **Deliverable:** the "perfect clone" for S25.

---

## 6. Agent Swarm Orchestration (for the new chat)

Recommended swarm structure — each agent is a self-contained workstream with a clear input/output contract. Run in this dependency order (parallel where noted):

| # | Agent | Task | Depends on | Parallel with |
|---|---|---|---|---|
| 1 | **API Contract Agent** | Extract the 488-endpoint contract + companion bridge into an OpenAPI spec / Kotlin client stubs | Upstream repo | — |
| 2 | **Scaffold Agent** | Create the Android project skeleton (Gradle, Compose, Ktor, Room, DataStore, Keystore) | — | 1 |
| 3 | **WebView Bridge Agent** | Phase 0 WebView wrapper + pairing | 2 | 1 |
| 4 | **Auth Agent** | Device-flow OAuth + session/token + 2FA in client | 1, 2 | 3 |
| 5 | **Chat/Streaming Agent** | SSE streaming client + chat UI + markdown | 1, 2 | 4 |
| 6 | **Notes/Tasks/Docs Agent** | Offline-first Room store + sync + editor | 2 | 5 |
| 7 | **On-device Model Agent** | llama.cpp/MediaPipe + GGUF download + FGS | 2 | 5, 6 |
| 8 | **Email/Calendar Agent** | IMAP/SMTP/CalDAV sync + offline cache | 1, 2 | 7 |
| 9 | **S25 Polish Agent** | DeX, One UI, widgets, NPU, battery | 2 (late) | 8 |
| 10 | **Compliance Agent** | AGPL notices, About screen, privacy policy, rebrand | — | all |
| 11 | **QA/Test Agent** | Test matrix: streaming fidelity, battery, offline, DeX | 3–9 | — |

**Sequencing guidance for the new chat:**
1. Launch Agents 1 + 2 + 10 in parallel (foundation + contract + compliance are independent).
2. On 1+2 completion, launch 3, 4, 5, 6, 7 in parallel (Phase 0–1).
3. On 5–7 completion, launch 8, 9 (Phase 2–3).
4. QA Agent (11) runs continuously against each phase's output.
5. Each agent writes its output to a shared `mobdysseus/` workspace and a `HANDOVER.md` so the swarm stays coherent.

---

## 7. Licensing & Compliance (mandatory, non-negotiable)

**AGPL-3.0-or-later.** The license is *not* an obstacle, but these are hard requirements:

1. **Publish full buildable source** (all Android code, Gradle/NDK/CMake, codegen) in a public repo; link it from the Play listing **and** the in-app About screen (§6(d)). Keep it up 3+ years.
2. **Keep the whole APK AGPL** — no proprietary EULA, no redistribution bans (§10).
3. **Ship the full AGPL text** + preserve all upstream copyright notices + `ACKNOWLEDGMENTS.md`/`licenses/`.
4. **Add a modification notice** with date (§5(a)) and an in-app "Open Source Licenses / About" screen with Appropriate Legal Notices (§5(d)).
5. **No DRM/anti-tamper** that blocks installing a modified rebuild (§6 User Product / Installation Information).
6. **PyMuPDF decision:** exclude it from the APK (keep the MIT-compatible core) — do not bundle AGPL PyMuPDF unless you accept full AGPL + §13 for form-filling.
7. **Keep `caldav` under Apache-2.0** (not its GPL option).
8. **§13 (Affero network clause) binds the server, not the client APK** — but if you ever host a *modified* Odysseus backend, every remote user must be offered source.
9. **Rebrand** — do not reuse the "Odysseus" name/wordmark (trademark + Play impersonation risk). Use a distinct name + "unofficial/community build" qualifier + distinct logo.
10. **Play Store:** privacy policy, accurate Data Safety form, **AI-content reporting/flagging + disclosure** (generative-AI policy applies even to on-device models).

---

## 8. Acceptance Criteria ("perfect clone" for S25)

- **Performance:** cold start <1s; first token <500ms (server) / <2s (on-device); idle <300MB RAM; <5%/hr active battery.
- **On-device AI:** 3B–4B Q4 GGUF at ≥10 tok/s; NPU ≥80% of ops via QNN/ExecuTorch where supported.
- **S25 integration:** full DeX (resizable, multi-window, keyboard/mouse); One UI edge panels, S Pen, Material You dynamic color, home-screen widgets.
- **Offline:** notes/tasks/calendar/email cache + on-device LLM work without a server.
- **Fidelity:** chat streaming, markdown, model picker, sessions, and agent output match the web app.
- **Compliance:** all §7 items satisfied before any distribution.

---

## 9. Risks & Mitigations

| Risk | Severity | Mitigation |
|---|---|---|
| Chat streaming fidelity (SSE) | High | Reuse server SSE contract exactly; test against live server |
| On-device NPU inference | High | Ship CPU/GPU llama.cpp first; NPU (QNN) as Phase 4 enhancement |
| Email background sync | High | WorkManager + FGS + graceful restart; server-side polling as fallback |
| Samsung One UI memory killer | Medium | Foreground service + partial wakelock + on-demand model load |
| AGPL copyleft violation | High | Publish full source, link everywhere, stay AGPL (§7) |
| Trademark/impersonation | Medium-High | Rebrand (§7.9) |
| Play AI-content/user-data policy | Medium | Reporting/flagging + privacy policy + Data Safety (§7.10) |
| Scope creep (50 features) | Medium | Strict phase gating; MVP = chat+notes+docs |

---

## 10. Immediate Next Steps (for the new chat)

1. Read this file + the upstream repo at `/root/mobdysseus`.
2. Launch the swarm per §6 (Agents 1, 2, 10 first).
3. Produce Phase 0 deliverable (WebView bridge APK) within 2 weeks.
4. Iterate through Phases 1–4, with QA Agent gating each phase.
5. Run the §7 compliance pass before any distribution.

---

*Research sources: upstream repo inspection (FastAPI/488 endpoints/SQLAlchemy/ChromaDB/vanilla-JS frontend), Galaxy S25 hardware/LLM feasibility research, AGPL-3.0 license analysis, and Android rebuild-strategy comparison. All five research reports were consolidated into this single plan.*
