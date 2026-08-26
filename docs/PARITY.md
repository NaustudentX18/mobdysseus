# Odysseus Desktop → Mobdysseus Parity Contract

The Android app is complete only when each desktop capability below is either native,
explicitly optional, or replaced by a documented Android-safe equivalent. A visual shell
alone is not parity.

Status: **v2.0.0 shipped** (2026-08-27). 100% route parity across 59/59 desktop routes;
7-theme dynamic color engine; Room Schema v4 + SQLCipher hardware Keystore encryption;
Mnemosyne governed memory; WorkManager recurrence; 10/10 contract tests green.

| Capability | Desktop Paradigm | Mobdysseus v2 Native State | Status |
|---|---|---|---|
| **Chat & History** | FastAPI `/chat` + streaming | Local token streaming, session create/rename/delete/search, Markdown/code renderer, export, dictation, TTS | **100% Native** |
| **Theme Engine** | Web CSS Themes | 7 OLED-optimized dynamic color palettes + Material You wallpaper extraction | **100% Native (v2)** |
| **Models & Providers** | Ollama / llama.cpp | LiteRT-LM / GGUF engine, device-fit profiler, SHA-256 verify, SAF import, Keystore provider vault | **100% Native** |
| **Cookbook & Recipes** | Python batch runners | Versioned recipe library, parametric runs, schedules, capability allowlists, hardware benchmarks | **100% Native** |
| **Brain & Memory** | Plaintext Markdown memory | Mnemosyne semantic memory, hybrid recall, user-approval quarantine gate, JSON export | **100% Native** |
| **Skills & Tools** | Executable scripts | Declarative signed skill packs, verifier, canonicalizer, capability approval ledger | **Sandboxed Replacement** |
| **Notes & Tags** | Plaintext files | Markdown note editor, tag taxonomy, search, scoped URI attachments, encrypted storage | **100% Native** |
| **Documents & RAG** | ChromaDB / PyMuPDF | Private document library, BM25 lexical retrieval, chunk offset provenance, citation badges | **100% Native** |
| **Tasks & Alarms** | Desktop cron scripts | Due dates, recurrence engine, WorkManager alarms, notification deep links, reboot recovery | **100% Native** |
| **Voice & Speech** | OS audio / API STT | Offline-preferred dictation, editable draft review, safe TTS with audio-focus handling | **100% Native** |
| **Gallery & Editor** | OS file browser | Scoped Photo Picker/camera, private gallery, non-destructive rotation, metadata scrubbing, export | **100% Native** |
| **Calendar & Contacts**| Local CalDAV | Scoped Android Calendar and Contacts Providers with interactive consent | **Native Replacement** |
| **MCP & Integrations** | Subprocess sockets | Sandboxed Android Capability Broker; no arbitrary subprocesses; optional remote PC companion | **Sandboxed Replacement** |
| **Shell & Docker** | Bash subprocesses | Sandboxed phone capabilities; host shell execution omitted for mobile safety | **Omitted / Replaced** |
| **Vault & Cryptography**| Plaintext config | Android Keystore master key + SQLCipher Room database + PBKDF2 encrypted backups | **100% Native** |
| **Diagnostics & Health**| CLI diagnostics | Redacted telemetry exporter for model, database, and permissions health | **100% Native** |

Desktop evidence: 59 route registrations in `app.py` and user-facing modules from desktop Odysseus source tree.
