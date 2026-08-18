# Mobdysseus

Native, standalone Android reconstruction of the desktop Odysseus workspace for personal use.

This project intentionally contains no `WebView`, remote server URL, bundled API key,
or required PC/Pi dependency. The first milestone establishes the S25-native workspace
shell and product navigation; subsequent milestones add local encrypted data, models,
memory, files, and Android capability adapters.

Build a debug APK:

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.
