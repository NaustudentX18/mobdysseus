# Contributing to Mobdysseus

Thanks for helping. Mobdysseus is a mobile rebuild of the
[Odysseus](https://github.com/odysseus-dev/odysseus) self-hosted AI workspace,
targeting the Samsung Galaxy S25.

## Ground rules

- The project is **AGPL-3.0-or-later**. Any contribution you make is licensed
  under it too. Keep third-party dependencies AGPL-compatible (MIT / BSD /
  Apache-2.0 / MPL-2.0 are fine).
- Keep branding distinct from upstream "Odysseus" (this is a community build).

## Getting started

```bash
git clone <this-repo>
cd mobdysseus
# ensure ANDROID_HOME points at an SDK with android-36 platform + build-tools 36
./gradlew :app:assembleDebug
```

Toolchain: JDK 17, Gradle 8.14 (wrapper), AGP 8.11.1, Kotlin 2.2.20, Compose BOM 2025.06.01.

## Where things live

- `app/src/main/java/com/mobdysseus/app/` — Kotlin sources
  - `provider/` — OpenAI-compatible provider client (`ProviderAdapter`, `ProviderConfig`)
  - `data/` — local persistence (`NotesStore`)
  - `ui/` — Compose screens (`MainScreen`, `ChatScreen`, `NotesScreen`, `SettingsScreen`)
  - `theme/` — the forest-green Material 3 theme
- `docs/` — plan, architecture, cookbook spec, handover, smoke test
- `reference/` — a research copy of the upstream Odysseus repo (gitignored)

## Style

- Kotlin, Jetpack Compose, Material 3.
- Keep the provider client dependency-light (stdlib + `org.json` only — no
  HTTP client library needed).
- Prefer small, focused composables.

## Before you submit

- `./gradlew :app:assembleDebug` must pass clean.
- For release, the keystore defaults are in `app/build.gradle.kts`
  (env `MOBDYSSEUS_KEYSTORE_PASS` overrides the default). Do not commit the keystore.
