# Mobdysseus — Attack Plan (Remaining Work → "Firing")

Status: **v0.5.0 shipped.** Standalone on-device chat (llama.cpp GGUF), notes, tasks,
documents, markdown, Cookbook, drawer nav, dark theme, signed APK, repo + CI + docs.

This is the execution plan for everything **left**, written so an agent swarm can
carry it out with zero re-derivation. Each workstream has a clear contract: goal,
files it owns, and a concrete verification step.

---

## 0. Locked decisions (do NOT re-litigate in the swarm)

| Topic | Decision |
|---|---|
| Architecture | Standalone-first; optional connect to server for LLM + MCP. No mandatory server. |
| On-device runtime | `llmedge` (`io.github.aatricks:llmedge:0.4.7.2`, Apache-2.0) — llama.cpp GGUF. |
| Remote LLM | OpenAI-compatible `/v1/chat/completions` (already in `ProviderAdapter`). |
| MCP transport | JSON-RPC 2.0 over **Streamable HTTP** (primary) + **SSE** (fallback) + **stdio** (future). |
| Model sweet spot | 3B–4B Q4_K_M GGUF; Q4_0 for Adreno GPU offload. |
| minSdk / target | minSdk **30** (llmedge) / targetSdk **36** (S25 = Android 15). |
| Persistence | JSON files via `data/*Store` (established). Move to Room only if a workstream needs querying. |
| License | Whole APK AGPL-3.0-or-later; third-party libs (llmedge Apache-2.0, mikepenz markdown) via About screen. |

---

## 1. Current state vs. target (delta)

**Done (v0.5.0):** Chat (SSE + on-device + markdown + history), Notes, Tasks, Documents,
Cookbook (recommend engine + tests), Settings (provider + on-device model), drawer nav,
signed APK, CI, AGPL license, docs.

**Remaining (this plan):**

| # | Area | Status |
|---|---|---|
| A | Server connection — **MCP client** | ❌ |
| A | Server connection — remote LLM presets + health | ⚠ partial (Ollama preset) |
| B | Calendar (offline) | ❌ |
| B | Memory (knowledge store) | ❌ |
| B | Research (web search) | ❌ |
| B | Gallery | ❌ |
| B | Email (IMAP/SMTP) | ❌ |
| B | Agents (server loop) | ❌ |
| C | On-device hardening — FGS, download manager, chat-template sessions | ⚠ partial |
| D | S25 polish — DeX, One UI, battery | ❌ |
| E | Compliance + professional repo (gating) | ⚠ partial |
| F | Device verification on S25 | ❌ |

---

## 2. Phase A — Server connection (the "capable to connect" half)

**A1 · MCP client core** (pure Kotlin, unit-testable — no UI)
- Files: `mcp/McpMessage.kt` (JSON-RPC 2.0 types), `mcp/McpClient.kt` (transport),
  `mcp/McpTransport.kt` (HTTP + SSE), `mcp/McpServerConfig.kt`, `data/McpServerStore.kt`.
- Contract: `tools/list`, `tools/call`, `initialize`, SSE streaming via
  `Accept: text/event-stream`; JSON-RPC ids, errors, timeouts.
- Verify: `McpClientTest` (JSON-RPC encode/decode round-trip + a mock HTTP/SSE server).

**A2 · MCP UI** — list/configure servers, discover tools, invoke a tool, show result.
- Files: `ui/McpScreen.kt`, drawer entry "MCP".
- Verify: compile + manual flow (add server → list tools → call → render result).

**A3 · Remote LLM presets + server profiles**
- Files: extend `provider/ProviderConfig.kt` (server profiles: name + baseUrl + model +
  optional MCP server list); Settings UI.
- Add presets: Ollama (exists), and generic "Self-hosted (custom)" for Hermes/grok2api.
- Verify: `ProviderAdapter` already streams; confirm preset round-trip via `ProviderStore`.

---

## 3. Phase B — Remaining Odysseus features (offline-first)

| Workstream | Files (new) | Core behavior | Verify |
|---|---|---|---|
| **B1 Calendar** | `data/CalendarStore.kt`, `ui/CalendarScreen.kt`, `data/Event` | local events, month view, reminders (AlarmManager) | compile + store round-trip test |
| **B2 Memory** | `data/MemoryStore.kt`, `ui/MemoryScreen.kt` | free-form knowledge entries + full-text search | store test |
| **B3 Research** | `research/SearchClient.kt`, `ui/ResearchScreen.kt` | web search (DuckDuckGo IA or configured API) → markdown report | mock-response test |
| **B4 Gallery** | `ui/GalleryScreen.kt` | scan `MediaStore`, grid + viewer | compile |
| **B5 Email** | `email/ImapClient.kt`, `ui/EmailScreen.kt` | IMAP fetch/SMTP send via server, offline cache | deferred — last; largest |
| **B6 Agents** | `ui/AgentsScreen.kt` | list/run/monitor agent loops (server via companion or MCP) | depends on A1 |

Order: B1 → B2/B4 (parallel) → B3 → B6 → B5 (last).

---

## 4. Phase C — On-device inference hardening

- **C1 Foreground service + wakelock + restart** (`service/InferenceService.kt`):
  `specialUse` FGS type, partial wakelock, `START_STICKY`, one-UI-killer mitigation.
- **C2 Model download manager** (`local/ModelDownloader.kt`): explicit prefetch via
  `edge.models.prefetch(...)` with progress callbacks + persisted cache; wire into Cookbook
  ("Download" button per recommendation).
- **C3 Chat-template sessions**: replace the raw-prompt `LocalLlmEngine` with
  `edge.text.session(...)` (correct multi-turn templates); reconcile with `ChatStore` history.
- **C4 GPU/NPU exploration** (stretch): Adreno OpenCL offload (Q4_0), per `llama-adreno`
  findings (44.8 t/s target); ExecuTorch/QNN only if time allows.
- Verify: C1/C2 compile + lifecycle reasoning; C3 behavior test on device (F1).

---

## 5. Phase D — S25 polish

- **D1 DeX**: `resizableActivity`, adaptive `WindowSizeClass` layouts, keyboard/mouse nav.
- **D2 One UI**: dynamic color (Material You) audit, edge panel + home-screen widget +
  S Pen input (stretch), app widget for tasks.
- **D3 Perf/battery**: measure cold start <1s, idle <300 MB, active <5%/hr; profile FGS.

---

## 6. Phase E — Compliance + professional repo (NON-NEGOTIABLE, gates release)

- **E1 In-app About / Open-Source Licenses screen** (AGPL text + llmedge Apache-2.0 +
  mikepenz markdown Apache-2.0 + Compose/ML Kit notices + modification date).
- **E2 `THIRD_PARTY_NOTICES.md` / `ACKNOWLEDGMENTS.md`** in repo root.
- **E3 Privacy policy** (local-only data by default; network only when user configures a server) + **Play Data Safety** form + **AI-content disclosure/flagging** in-app.
- **E4 Publish full buildable source** to a public repo; link it from About + listing.
- **E5 CI**: extend `.github/workflows/ci.yml` to run `testDebugUnitTest` + `assembleRelease` (signed via secret) and assert the APK exists.
- Verify: a fresh `git clone` + `./gradlew assembleRelease` succeeds from scratch.

---

## 7. Phase F — Verification & ship ("firing")

- **F1 Device install** on S25 (`adb install dist/Mobdysseus-*.apk`).
- **F2 Smoke test** (refresh `docs/SMOKE-TEST.md`): offline chat (tok/s), notes/tasks/docs,
  calendar, MCP tool call, on-device model download + inference, battery.
- **F3 Release**: bump version, tag `v1.0.0`, signed APK in `dist/`, SHA-256 recorded.

**Definition of "firing":** F1 + F2 green + E1–E4 done + `v1.0.0` signed APK + public source.

---

## 8. Agent swarm assignment

Each agent = one workstream; owns its files; writes `HANDOVER.md` note; verification is part of its contract.

| # | Agent | Workstream | Depends on | Parallel with |
|---|---|---|---|---|
| 1 | **MCP Core Agent** | A1 (McpClient + store + tests) | — | 4, 5 |
| 2 | **MCP UI Agent** | A2 (McpScreen + drawer) | 1 | 3 |
| 3 | **Server Profiles Agent** | A3 (server profiles + presets) | — | 2, 6 |
| 4 | **Calendar Agent** | B1 | — | 1, 5 |
| 5 | **Memory Agent** | B2 | — | 1, 4 |
| 6 | **Research Agent** | B3 | — | 3 |
| 7 | **Gallery Agent** | B4 | — | 6 |
| 8 | **Agents Agent** | B6 | 1, 3 | 9 |
| 9 | **Email Agent** | B5 | 1, 3 | 8 |
| 10 | **Inference Hardening Agent** | C1–C4 | — | 1–4 |
| 11 | **S25 Polish Agent** | D1–D3 | late | 2–9 |
| 12 | **Compliance Agent** | E1–E5 | — | all |
| 13 | **QA/Verify Agent** | F1–F3 + expand unit tests | 1–11 | continuous |

**Sequencing:**
1. Launch Agents 1, 3, 4, 5, 10, 12 in parallel (independent).
2. On 1+3: launch 2, 6, 8, 9 (MCP UI, research, agents, email).
3. On 2–9: launch 11 (polish) — it needs the feature set stable.
4. QA Agent (13) gates every phase and drives the F1 device pass.

---

## 9. Risks

| Risk | Mitigation |
|---|---|
| llmedge on-device not device-tested | F1 early; keep remote fallback (already present) |
| MCP transport variance (HTTP vs SSE) | A1 supports both; test with mock + a live Pi bridge |
| Email scope (largest) | B5 last; server-side polling first |
| APK size creep (129 MB) | Keep excludes; R8/minify as a separate hardening step |
| Compliance miss | E-phase is a hard gate before any `v1.0.0` tag |

---

*Replaces the phased roadmap in `PLAN.md` §5–6 for the post-v0.5.0 reality; keeps
`COOKBOOK_SPEC.md`, `ARCHITECTURE.md`, and `SMOKE-TEST.md` as source of truth.*
