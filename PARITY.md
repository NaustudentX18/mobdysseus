# Odysseus Desktop → Mobdysseus parity contract

The Android app is complete only when each desktop capability below is either native,
explicitly optional, or replaced by a documented Android-safe equivalent. A visual shell
alone is not parity.

Status: **v1.0.0 shipped** (2026-08-18). Signed release APK on the PC desktop. 118 unit
tests + 7 contract tests green; 59/59 desktop routes classified; dependency boundaries pass.

| Capability | Android state | Required native equivalent |
|---|---|---|
| Chat, sessions, history | Partial | Streaming local chat, session create/select/rename/delete/search, copy/share/retry, export, dictation, TTS. Attachments still pending. |
| Models and providers | Partial | LiteRT-LM model library, device-fit, health, selection, SHA-256 verify, SAF import; optional secrets vault. |
| Cookbook and recipes | Partial | Recipe library, runs, schedules, model catalog. Output/timing history pending. |
| Brain and memory | Partial | Semantic recall, add/edit/delete, search, export, governance + review gate. Import/extraction pending. |
| Skills and tools | Partial | Declarative skill library + verifier + canonicalizer; permissioned capability registry + approval ledger. |
| Notes | Partial | Editing, search, Markdown, tags, export/share. Folders + attachments pending. |
| Documents and RAG | Partial | Private file library, ingestion, lexical retrieval, citations. Full PDF/DOCX/OCR pipeline pending. |
| Tasks and automation | Partial | Due dates, recurrence, WorkManager reminders, notifications, deep links, run history. |
| Voice | Partial | Offline-preferred dictation, editable transcript, TTS with stop/audio-focus. Voice routing pending. |
| Search and research | Missing (optional) | Optional network research plus offline archive and citations; gated behind local-only. |
| Gallery and image editor | Partial | Camera/photo picker, private gallery, rotate/edit, export. Full touch editor pending. |
| Calendar and contacts | Partial | Android providers behind scoped runtime permission; picker + create. |
| Email | Missing (optional) | Optional OAuth/IMAP integration and native composer; gated behind local-only. |
| Personal files | Partial | Private document store, metadata, share-in. |
| MCP and integrations | Partial | Mobile capability broker; no arbitrary subprocess execution. |
| Shell/Docker/workspaces | Replacement required | Sandboxed phone actions; optional explicit PC pairing only. |
| Reminders/webhooks | Partial | WorkManager, notifications, permissioned signed webhook delivery. |
| Auth/tokens/vault | Partial | Keystore vault, scoped integration-secret UI. Biometric lock pending. |
| Backup/preferences/themes | Partial | Encrypted backup/restore, restore validation, migrations, granular controls. |
| Security/diagnostics | Partial | Permission ledger, runtime health, data controls, redacted diagnostics export. |

Desktop evidence: router registrations in `app.py` and user-facing modules in
`static/index.html` and `static/js/` from the desktop Odysseus source tree.
