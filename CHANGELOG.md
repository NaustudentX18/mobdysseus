# Changelog

## [0.8.0] — 2025-08-26
### Added
- **Skills** tab + marketplace: browse and install skills (Ask Your Data, Privacy Verdict, One-Tap Capture, Photo OCR, plus roadmap skills).
- **Cookbook**: model cards are now clickable — download a recommended GGUF with live progress, then it's selected for Chat.

### Fixed
- **On-device inference crash**: the app could shut down when answering with a downloaded local model and then fail to reopen. The LLM engine is now loaded lazily (off the main thread) so a corrupt/oversized model can never crash the app at startup; all llmedge failures (memory, corrupt model, worker crash) are caught and shown as friendly messages; the cached GGUF is validated (size + magic bytes) and a corrupt file is deleted so it can be re-downloaded.

### Changed
- Version 0.8.0 (versionCode 9).

All notable changes. Mobdysseus follows [SemVer](https://semver.org). APKs ship in `dist/`.

## [0.7.1] — 2025-08-26
### Added
- Samsung DeX / multi-window support (`resizeableActivity`).
- `CHANGELOG.md`.
### Changed
- CI: added lint step + optional signed-release step.
- Fixed Calendar `LocalDate.ofInstant` (API 34) → `atZone().toLocalDate()` so it runs on minSdk 30.

All notable changes. Mobdysseus follows [SemVer](https://semver.org). APKs ship in `dist/`.

## [0.7.0] — 2025-08-26
### Added
- **Research** (B3): DuckDuckGo Instant Answer web search (`SearchClient` + `ResearchScreen` + unit tests).
- **Gallery** (B4): on-device photo grid + full-screen viewer (`MediaStore`, `READ_MEDIA_IMAGES`).
- Root `README.md`; rewritten `docs/SMOKE-TEST.md`.

### Changed
- On-device chat bounds multi-turn history to the last 8 messages to stay within the token budget.
- Drawer now lists: Chat, Notes, Documents, Tasks, Calendar, Memory, Gallery, Research, Cookbook, MCP Tools, Settings.

## [0.6.0] — 2025-08-26
### Added
- **Calendar** (B1): month grid + events (`CalendarStore`/`EventCodec`/`CalendarScreen`).
- **Memory** (B2): knowledge entries + search (`MemoryStore`/`MemoryCodec`/`MemoryScreen`).
- **MCP tools** (A1+A2): JSON-RPC/SSE client (`McpClient`/`McpTypes`/`McpServerStore`) + server/tool UI (`McpScreen`).
- **Inference hardening** (C1+C2): `InferenceService` foreground service + wakelock; `ModelDownloadManager`.
- Settings **Test connection** for cloud providers.
- Compliance: `THIRD_PARTY_NOTICES.md`, `docs/PRIVACY.md`, `licenses/APACHE-2.0.txt`, About screen content.
- Repo hardening: `scripts/guard.sh` + `.gitignore` for symlink/temp-overlay leftovers; `docs/DEV-GUIDE.md`; CI guard step; `org.json` test dependency.

## [0.5.0] — 2025-08-25
### Added
- On-device LLM (llama.cpp GGUF via `llmedge`) with model picker in Settings.
- Full offline chat (no server required).

## [0.4.0] — 2025-08-25
### Added
- Cookbook: hardware detection + model ranking.

## [0.3.0] — 2025-08-25
### Added
- Documents (markdown storage + rendering).

## [0.2.0] — 2025-08-25
### Added
- Tasks, dark theme.

## [0.1.0] — 2025-08-25
### Added
- Chat (OpenAI-compatible SSE), Notes, Settings/provider presets, drawer nav, signed APK scaffold.
