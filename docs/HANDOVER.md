# Mobdysseus — Project Handover / Briefing
_Hand this file to a fresh assistant (or the build swarm) and they will know exactly what's done, what the app is, and what to do next._

**Project root:** `/root/mobdysseus/`
**App:** Mobdysseus · package `com.mobdysseus.app` · label **Mobdysseus**
**Target:** Samsung Galaxy S25 (Android 15, ARM aarch64). Standalone APK.
**Basis:** a native Kotlin/Compose rebuild of `odysseus-dev/odysseus` (self-hosted AI workspace), AGPL-3.0-or-later.

---

## 0. TL;DR — what's done
1. ✅ **Audit + plan phase complete**: upstream Odysseus reverse-audited (FastAPI, 488 endpoints, SQLAlchemy+ChromaDB, vanilla-JS frontend), Galaxy S25 platform researched, licensing (AGPL) mapped. → `docs/PLAN.md` + `docs/COOKBOOK_SPEC.md`.
2. ✅ **v0.1.0 APK shipped**: native Kotlin+Compose app with **streaming chat** (OpenAI-compatible `ProviderAdapter`), **local notes**, **provider settings**. Signed (`CN=Mobdysseus`), in `dist/Mobdysseus-v0.1.0.apk` + `/sdcard/Download/`.
3. **REMAINING (human/device):** install on the S25 and run `docs/SMOKE-TEST.md`; point Settings at a real provider (Ollama local `http://127.0.0.1:11434/v1` zero-key is easiest, or a keyed cloud provider).

---

## 1. The user's vision
- A **full rebuild of Odysseus** (self-hosted AI workspace) as an APK **exactly designed and optimised for the Galaxy S25**.
- "Fully clean, polished, shipped APK + professional-grade repo, as per every build we do" (the established OpenForest / OFH / clone-kit conventions).

## 2. Architecture decision (locked)
- **Hybrid client-server**: native Kotlin+Compose client → self-hosted Odysseus server via the `/api/companion/*` pairing bridge, with a thin **on-device model layer** (llama.cpp, 3B–4B Q4 GGUF) for offline.
- **v0.1.0** is the native client foundation only (no server pairing, no on-device model yet).

## 3. Toolchain (CRITICAL — proven on this host)
- **AGP 8.11.1 + Gradle 8.14 (wrapper) + Kotlin 2.2.20, JVM 17.** compileSdk **36**, minSdk **26**, targetSdk **36**.
- Compose BOM **2025.06.01**, Material3 + material-icons-core.
- Signing: `release.keystore` (alias `mobdysseus`, pass `mobdysseus123`), env `MOBDYSSEUS_KEYSTORE_PASS` overrides. v2+v3 via Gradle (no manual apksigner needed).
- SDK at `/root/Android` (platform android-36, build-tools 36.0.0). `local.properties` points `sdk.dir=/root/Android`.

## 4. File map
```
README.md, CONTRIBUTING.md, LICENSE (AGPL-3.0), .gitignore
settings.gradle.kts, build.gradle.kts, gradle.properties, local.properties
gradle/libs.versions.toml, gradle/wrapper/*
release.keystore                       # alias mobdysseus / mobdysseus123
dist/Mobdysseus-v0.1.0.apk
docs/{PLAN,COOKBOOK_SPEC,ARCHITECTURE,HANDOVER,SMOKE-TEST}.md
.github/workflows/ci.yml
app/build.gradle.kts, app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/mobdysseus/app/
  MainActivity.kt
  theme/Theme.kt                       # dark forest-green
  provider/{ProviderConfig,ProviderAdapter}.kt
  data/NotesStore.kt
  ui/{MainScreen,ChatScreen,NotesScreen,SettingsScreen}.kt
reference/                            # upstream Odysseus clone (gitignored, research only)
```

## 5. Build command cheat-sheet
```bash
cd /root/mobdysseus
./gradlew :app:assembleDebug --console=plain --no-daemon
./gradlew :app:assembleRelease --console=plain --no-daemon
cp app/build/outputs/apk/release/app-release.apk dist/Mobdysseus-v0.1.0.apk
```

## 6. CRITICAL gotchas (this environment)
- **`write`/`edit` tools create DANGLING SYMLINKS** for new files that real processes (gradle/git) can't read. **Create project files via `bash` heredoc** (`cat > path <<'EOF'`), then verify `[ -f X ] && [ ! -L X ]`.
- **`zipalign` in build-tools is x86-64** → can't run on aarch64. **Rely on Gradle's built-in zipalign** (part of `packageRelease`). Do NOT manual-zipalign.
- Each `bash` call is a fresh shell; the Gradle daemon does not persist (`--no-daemon`).
- `reference/` is a gitignored research copy — do not edit it.

## 7. Next steps (for the swarm)
1. Run `docs/SMOKE-TEST.md` on-device; fix anything.
2. **Phase 1**: server pairing (device-flow OAuth against `/api/companion/*`), sessions, markdown rendering, model picker.
3. **Phase 2**: on-device Cookbook (llama.cpp GGUF) — see `docs/COOKBOOK_SPEC.md`.
4. **Phase 3–4**: email/calendar/gallery/agents, then DeX / One UI / NPU polish.
5. Compliance pass (AGPL notices, About screen, rebrand, privacy policy) before any store release — see `docs/PLAN.md` §7.
