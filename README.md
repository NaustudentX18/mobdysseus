# Mobdysseus

A **native Android rebuild** of [Odysseus](https://github.com/odysseus-dev/odysseus) —
the self-hosted AI workspace — designed and optimised for the **Samsung Galaxy S25**.

Mobdysseus is a community build. It is **not** affiliated with the upstream
Odysseus project, and it ships under the same **AGPL-3.0-or-later** license.

## Status

**v0.4.0** — native Kotlin + Jetpack Compose app with streaming AI chat
(Markdown rendering + persistent history), notes, documents, tasks, provider
configuration, and an on-device model Cookbook.

| Feature | Status |
|---|---|
| Streaming chat (OpenAI-compatible providers) | ✅ |
| Provider presets (Ollama local, DeepSeek, OpenAI, custom) | ✅ |
| Local notes (create / edit / delete) | ✅ |
| Tasks (todos with completion) | ✅ |
| Documents (Markdown editor with preview) | ✅ |
| Markdown rendering in chat | ✅ |
| Chat history persistence | ✅ |
| Settings + about | ✅ |
| Model Cookbook (hardware detection + ranked recommendations) | ✅ |
| On-device LLM serving (llama.cpp GGUF) | 🚧 planned (Phase 2) |
| Documents / Email / Calendar / Agents | 🚧 planned (Phase 3) |

## What Odysseus is

Upstream Odysseus is a self-hosted AI workspace: chat + agents, deep research,
documents, email, notes/tasks/calendar, gallery, and local-model workflows,
served by a Python/FastAPI backend (~488 HTTP endpoints) with a vanilla-JS web
client. Mobdysseus rebuilds that experience as a native Android client.

## Architecture

- **Native UI** — Kotlin + Jetpack Compose (Material 3), forest-green dark theme.
- **Provider client** — a dependency-light OpenAI-compatible client
  (`ProviderAdapter`) that streams chat via SSE against any
  `/v1/chat/completions` endpoint (OpenAI, DeepSeek, Ollama, local servers…).
- **Local-first** — notes persist to app-private storage (`NotesStore`).
- See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and
  [`docs/PLAN.md`](docs/PLAN.md) for the full roadmap and the on-device
  Cookbook spec ([`docs/COOKBOOK_SPEC.md`](docs/COOKBOOK_SPEC.md)).

## Build

Toolchain (verified): JDK 17, Gradle 8.14 (wrapper), AGP 8.11.1, Kotlin 2.2.20,
Compose BOM 2025.06.01, compileSdk 36 / minSdk 26 / targetSdk 36.

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # signed release (keystore defaults in app/build.gradle.kts)
```

The signed release APK lands in `app/build/outputs/apk/release/`; a copy is kept
in `dist/` and pushed to `/sdcard/Download` for on-device install.

## License

AGPL-3.0-or-later — see [LICENSE](LICENSE). Upstream attribution and third-party
notices are preserved in [`reference/ACKNOWLEDGMENTS.md`](reference/ACKNOWLEDGMENTS.md).

> ⚠️ This is a community/unofficial build of an AGPL project. The name
> "Mobdysseus" is distinct from upstream "Odysseus" to avoid confusion; all
> copyright notices and the AGPL license text are retained.
