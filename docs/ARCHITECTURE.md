# Mobdysseus — Architecture

## Overview

Mobdysseus is a single-module Android app (`:app`) in Kotlin + Jetpack Compose.
It is a **native client** whose core feature is streaming chat against any
OpenAI-compatible provider, plus local notes and provider settings.

```
┌─────────────────────────────────────────────────────┐
│ MainActivity (ComponentActivity)                    │
│   └─ MobdysseusTheme (dark forest-green Material 3) │
│       └─ MainScreen (Scaffold + NavigationBar)      │
│           ├─ ChatScreen      → ProviderAdapter      │
│           ├─ NotesScreen     → NotesStore           │
│           └─ SettingsScreen  → ProviderStore        │
└─────────────────────────────────────────────────────┘
```

## Modules (packages under `com.mobdysseus.app`)

| Package | Responsibility |
|---|---|
| `theme/` | `MobdysseusTheme`, dark green color scheme |
| `provider/` | `ProviderConfig` (data + presets), `ProviderStore` (SharedPreferences persistence), `ProviderAdapter` (SSE streaming client), `ChatMessage` |
| `data/` | `NotesStore`, `TasksStore`, `ChatStore`, `DocumentsStore` (JSON file persistence) |
| `cookbook/` | `HardwareDetector`, `ModelRanker` (fit-scoring), `Catalog` (HF + curated) |
| `local/` | `LocalLlmEngine` (on-device GGUF via llmedge/llama.cpp JNI) |
| `ui/` | `MainScreen` (drawer nav), `ChatScreen`, `NotesScreen`, `DocumentsScreen`, `TasksScreen`, `CookbookScreen`, `SettingsScreen` |

## ProviderAdapter (streaming)

- Pure JDK networking (`HttpURLConnection`) + `org.json` — no HTTP library
  dependency, keeps the APK small.
- `stream(messages): Flow<String>` posts a `{model, stream:true, messages}`
  body to `<baseUrl>/chat/completions` and parses `data:` SSE frames, emitting
  each `choices[0].delta.content` token.
- Errors surface as a thrown `IllegalStateException`, rendered in the chat.

## Persistence

- Provider config → `SharedPreferences("provider")`.
- Notes → `filesDir/notes.json` (JSON array of `{id,title,body,updatedAt}`).
- Tasks → `filesDir/tasks.json` (JSON array of `{id,title,done,createdAt}`).
- Chat transcript → `filesDir/chat.json` (JSON array of `{role,content}`).
- Documents → `filesDir/documents.json` (JSON array of `{id,title,body,updatedAt}`).

## Extensibility (planned)

The roadmap (`docs/PLAN.md`) targets a hybrid client-server model: the app
talks to a self-hosted Odysseus server via the existing `/api/companion/*`
pairing bridge, with an on-device Cookbook (llama.cpp GGUF serving) as Phase 2.
See `docs/COOKBOOK_SPEC.md` for the full on-device Cookbook design.
