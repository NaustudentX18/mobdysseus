# Mobdysseus

**A mobile, standalone-first rebuild of the [Odysseus](https://github.com/odysseus-dev/odysseus) self-hosted AI workspace, designed and optimised for Samsung Galaxy S25.**

Mobdysseus runs fully on-device out of the box — notes, tasks, documents, calendar, memory, photo gallery, an AI chat powered by an on-device LLM (llama.cpp GGUF), and a cookbook that picks the right model for your hardware. It can also connect to a self-hosted server for a remote LLM and to [MCP](https://modelcontextprotocol.io) servers to reach external tools. No account, no cloud, no account, no telemetry.

## Screens / features

| Area | What it does |
|---|---|
| Chat | Streaming chat over OpenAI-compatible endpoints *and* on-device GGUF (llama.cpp via [llmedge](https://github.com/aatricks/llmedge)); markdown rendering; conversation history. |
| Notes / Tasks / Documents | Plain, offline JSON-backed stores (mirror `data/NotesStore.kt`). |
| Calendar | Month grid, events, add/edit/delete (offline). |
| Memory | Free-form knowledge entries with full-text search. |
| Gallery | Device photo grid + full-screen viewer (`MediaStore`). |
| Research | Web search (DuckDuckGo Instant Answer) rendered as cards. |
| Cookbook | Hardware detection + model recommendation tuned for Galaxy S25. |
| MCP Tools | Add self-hosted MCP servers, discover tools, invoke them, stream results. |
| Settings | Model source (On-device / Cloud API), provider presets, on-device GGUF picker, test connection, About/licenses. |

## Architecture

- **On-device first.** No mandatory server. The default LLM is a local GGUF (3B–4B Q4) run via `llmedge`; everything else is local JSON persistence.
- **Connect when you want.** Point Settings at any OpenAI-compatible endpoint, or add MCP servers, to pull in remote LLMs and tools.
- **Modules** live under `app/src/main/java/com/mobdysseus/app/`: `data` (stores), `ui` (screens), `provider` (remote LLM), `local` (on-device inference), `mcp` (client), `service` (foreground inference service), `cookbook` (model ranking), `research`.

## Build

Requires JDK 17, the Android SDK (compileSdk 36, minSdk 30), AGP 8.11.1 + Gradle 8.14 (wrapper included).

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:assembleDebug       # debug APK
./gradlew :app:assembleRelease     # signed release APK (release.keystore)
```

Release signing uses `release.keystore` (alias `mobdysseus`); password defaults to `mobdysseus123` or `MOBDYSSEUS_KEYSTORE_PASS`.

## Repo hygiene

- **`scripts/guard.sh`** fails if any symlink / temp-overlay leftover appears in `app/src`, `docs`, or `licenses`. It runs in CI.
- **`docs/DEV-GUIDE.md`** — the rule that files are created with bash heredocs, never the session `write`/`edit` tools.
- **`docs/`** — `PLAN.md`, `ARCHITECTURE.md`, `COOKBOOK_SPEC.md`, `SMOKE-TEST.md`, `HANDOVER.md`, `ATTACK-PLAN.md`, `PRIVACY.md`, `DEV-GUIDE.md`.
- **`THIRD_PARTY_NOTICES.md`** + **`licenses/`** — attribution for llmedge, Compose, ML Kit, and the bundled llama.cpp/ggml, whisper.cpp, bark.cpp.

## License

**AGPL-3.0-or-later.** This is a community build and is **not affiliated with** the upstream Odysseus project. Third-party attributions are in `THIRD_PARTY_NOTICES.md`.

>Your data stays on-device by default. See `docs/PRIVACY.md` for the three explicit cases where the app touches the network (a cloud API provider, an MCP server, or a model download).
