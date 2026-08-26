# Mobdysseus Contributor & Developer Guide

> **Guidelines for Human and AI Contributors to Mobdysseus.**

---

## 🛡️ Non-Negotiable Development Rules

1. **Native Android Only:** All UI code must be written in Kotlin and Jetpack Compose. WebViews, hybrid bridges, or browser runtimes are strictly prohibited.
2. **Offline-First Contract:** Every core capability (Chat, Notes, Tasks, Recipes, Brain Memory, RAG) must function with 100% fidelity in airplane mode with zero network access.
3. **Respect Dependency Boundaries:** Feature implementations must never import other feature implementations. Verify this with `python3 tools/check_dependency_boundaries.py`.
4. **No Secrets in Source or Logs:** Never log prompts, decrypted documents, keystore keys, or tokens to Android `Logcat`.
5. **Authoritative Builds on Windows:** The native Android build toolchain and Gradle release signing execute on the paired Windows PC build node (`desktop-ujsii52`).

---

## 🛠️ Verification Commands

Before submitting code, all local contract and boundary checks must pass:

```bash
# 1. Check dependency boundaries across all Kotlin files
python3 tools/check_dependency_boundaries.py

# 2. Check desktop capability inventory completeness
python3 tools/validate_desktop_capability_inventory.py

# 3. Run all Python contract tests
python3 -m pytest tools/ -v
```

---

## 🚀 Remote Build & Sync Workflow (OMP / Hermes Pattern)

To trigger authoritative Gradle compilation on the paired Windows host:

```bash
# 1. Sync source files (excluding local build caches)
rsync -avz --delete \
  --exclude '.gradle' --exclude '.kotlin' --exclude 'app/build' \
  --exclude 'local.properties' --exclude '*.hprof' \
  app/ pc:C:/Users/jakem/Projects/odysseus-mobile-native/app/

# 2. Trigger remote compilation and unit tests
ssh -F ~/.ssh/config pc "cd C:\\Users\\jakem\\Projects\\odysseus-mobile-native && gradlew.bat testDebugUnitTest assembleDebug"
```

---

## 🧩 Adding a New Feature or Capability

1. Define the immutable data model in `core/`.
2. If the feature requires Android system resources (e.g. Camera, Storage, Contacts), add a typed capability definition in `capability/CapabilityPolicy.kt` with clear rationale and security classification.
3. Expose the user interface in `feature/` using `MobdysseusThemeColors.current` for consistent theming.
4. Register navigation entry in `navigation/Destination.kt`.
5. Run `python3 tools/check_dependency_boundaries.py` to confirm zero circular dependencies.
