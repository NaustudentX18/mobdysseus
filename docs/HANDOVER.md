# Mobdysseus — Project Handover / Briefing
_Hand this to a fresh assistant (or the build swarm) and they know exactly what exists, where, and what to do next._

- **Project root:** `/root/mobdysseus/`
- **App:** Mobdysseus · package `com.mobdysseus.app` · label **Mobdysseus**
- **Repo (LIVE):** `github.com/NaustudentX18/mobdysseus` (PUBLIC, default branch `main`)
- **Target:** Samsung Galaxy S25 (Android 15, ARM aarch64). Standalone APK.
- **Basis:** native Kotlin/Compose rebuild of `odysseus-dev/odysseus` (self-hosted AI workspace), **AGPL-3.0-or-later**. Community build — not affiliated with upstream.

---

## 0. Current status — SHIPPED & PUBLISHED
- **v0.7.1** signed release APK in `dist/Mobdysseus-v0.7.1.apk` (SHA-256 `b3fba4923efd9f4b1ef3d7a751cb84ec0bd284e4c7a7777f7a42042771b92adc`), hosted on **GitHub Releases** (repo per-file limit is 100 MB, so the 129 MB APK lives on Releases, not in the tree).
- **Landing page is professional:** custom logo (`assets/logo.png`, `assets/icon.png`), 6 feature screenshots (`assets/screenshots/`), badges, guides & advice, Roadmap. See `README.md`.
- **CI is GREEN** on every push: guard → unit tests → lint → debug APK → **signed release APK**.
- **Research team delivered 4 market-gap reports** → synthesised into `docs/ROADMAP.md` + a Roadmap section in the README.

## 1. Repo / GitHub state
- `gh` authed as **NaustudentX18** (scopes: repo, workflow, admin:org, etc.). `git remote origin` = the public repo.
- **Secrets set** (repo-level): `MOBDYSSEUS_KEYSTORE_BASE64` (base64 of `release.keystore`) and `MOBDYSSEUS_KEYSTORE_PASS` (`mobdysseus123`). These let CI sign release APKs.
- **Topics set:** android, kotlin, jetpack-compose, on-device-llm, llama-cpp, gguf, mcp, galaxy-s25, ai-workspace, odysseus.
- **GitHub Actions lesson (IMPORTANT):** you **cannot reference `secrets` inside an `if:` condition** — the workflow silently fails at 0s with "workflow file issue". Map secrets to a **job-level `env`** and use `if: ${{ env.KEYSTORE_B64 != '' }}` instead. See `.github/workflows/ci.yml`.

## 2. Architecture (locked)
- **Standalone-first** hybrid: native Kotlin/Compose client with a **on-device LLM** (llama.cpp via `io.github.aatricks:llmedge`, GGUF 3B–4B Q4), **optionally** connectable to a self-hosted server for a remote LLM (OpenAI-compatible SSE) and **MCP tools**. No account, no cloud, no telemetry by default.

## 3. Features (all shipped in app)
Chat (local GGUF + remote SSE), Notes, Tasks, Documents, Calendar, Memory (searchable knowledge store), Gallery, Research (DuckDuckGo), Cookbook (hardware-aware model ranking), MCP Tools (JSON-RPC/SSE client), Settings (model source, provider presets, GGUF picker, connection test, About+licenses). DeX/multi-window (`resizeableActivity`). **19 unit tests pass.**

## 4. Toolchain (proven on this host)
- AGP 8.11.1 + Gradle 8.14 (wrapper) + Kotlin 2.2.20, JVM 17. compileSdk **36**, minSdk **30**, targetSdk **36**.
- Compose BOM 2025.06.01, Material3 + material-icons-core. Tests: junit + org.json (testImplementation).
- `ndk { abiFilters += "arm64-v8a" }`; packaging excludes unused JNI libs (llmsdcpp.so, whisper_jni, bark_jni, onnxruntime, mlkit OCR/common).
- Signing: `release.keystore` (alias `mobdysseus`, pass `mobdysseus123` or env `MOBDYSSEUS_KEYSTORE_PASS`). `release.keystore` is **gitignored**.
- SDK at `/root/Android` (platform android-36, build-tools 36.0.0). `local.properties` → `sdk.dir=/root/Android`.
- Builds with `--no-daemon` (fresh shell each call).

## 5. Key file map
```
app/src/main/java/com/mobdysseus/app/
  MainActivity.kt, MainScreen.kt (Tab enum: Chat..Settings)
  theme/Theme.kt
  data/{Notes,Tasks,ChatStore,Documents,Calendar,Memory,McpServer}Store.kt
  ui/{Chat,Notes,Tasks,Documents,Cookbook,Settings,Calendar,Memory,Gallery,Research,Mcp}Screen.kt
  provider/{ProviderConfig,ProviderAdapter}.kt   # SSE remote LLM + healthCheck()
  local/{LocalLlmEngine,ModelDownloadManager}.kt  # llmedge on-device
  mcp/{McpTypes,McpClient}.kt
  service/InferenceService.kt  # foreground service + wakelock
  cookbook/, research/
docs/  PLAN, COOKBOOK_SPEC, ARCHITECTURE, HANDOVER, SMOKE-TEST, ATTACK-PLAN, PRIVACY, DEV-GUIDE, ABOUT_SCREEN_CONTENT, ROADMAP
scripts/ guard.sh, generate_screenshots.py
assets/ icon.*, logo.*, screenshots/
dist/   Mobdysseus-v0.7.1.apk
```

## 6. Build commands
```bash
cd /root/mobdysseus
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:assembleDebug --no-daemon
./gradlew :app:assembleRelease --no-daemon
cp app/build/outputs/apk/release/app-release.apk dist/Mobdysseus-vX.Y.Z.apk
```

## 7. CRITICAL gotchas (this environment)
- **`write`/`edit` tools create DANGLING SYMLINKS** for new files (they vanish later). **Create project files via `bash` heredoc** (`cat > path <<'EOF'`) or `python` `open(p,'w').write(...)`, then verify `[ -f X ] && [ ! -L X ]`. Same rule for any subagent you spawn — tell it explicitly. Run `bash scripts/guard.sh` before committing.
- **Long heredocs truncate** — for big files, append in chunks (unique delimiters) or write via python `open().write()`.
- `zipalign` in build-tools is x86-64 and can't run on aarch64 — rely on Gradle's built-in zipalign (do NOT manual-zipalign).
- Each `bash` call is a fresh shell (no persistent Gradle daemon).

## 8. Next steps for a new session (priority order)
1. **On-device smoke test (F1)** on the real S25 per `docs/SMOKE-TEST.md` — the only remaining runtime gap (this host has no phone).
2. **Roadmap (docs/ROADMAP.md)** — start Phase 0: Ask Your Data (on-device RAG), Privacy Verdict, One-Tap Capture, offline OCR; then Phase 1 memory graph + self-hosted sync; Phase 2 MCP trust (phone-as-server, permission gate, trust check); Phase 3 S25-native (voice, widgets, DeX panes).
3. **Bump version + rebuild + release** for each milestone; keep the APK on GitHub Releases.
4. Keep CI green; guard before every commit.

> **Always** create files via bash heredoc/python (never `write`/`edit`). See `docs/DEV-GUIDE.md`; run `bash scripts/guard.sh` before commits.
