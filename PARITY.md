# Odysseus Desktop → Mobdysseus parity contract

The Android app is complete only when each desktop capability below is either native,
explicitly optional, or replaced by a documented Android-safe equivalent. A visual shell
alone is not parity.

| Capability | Android state | Required native equivalent |
|---|---|---|
| Chat, sessions, history | Partial | Streaming local chat, history, search, attachments, export |
| Models and providers | Partial | LiteRT-LM model library, health, selection; optional secrets vault |
| Cookbook and recipes | Partial | Recipe library, execution, schedules, output history |
| Brain and memory | Partial | Semantic recall, edit/categories, import/export, extraction |
| Skills and tools | Missing | Permissioned Android capability registry and approval ledger |
| Notes | Partial | Editing, search, tags/folders, Markdown, attachments |
| Documents and RAG | Partial | Private file library, PDF/DOCX/image ingestion, citations |
| Tasks and automation | Partial | Due dates, recurrence, WorkManager, notifications, run history |
| Voice | Partial | Reliable offline STT, voice settings, voice routing |
| Search and research | Missing | Optional network research plus offline archive and citations |
| Gallery and image editor | Missing | Camera/photo picker, gallery and touch-native editing |
| Calendar and contacts | Missing | Android providers behind scoped runtime permission |
| Email | Missing | Optional OAuth/IMAP integration and native composer |
| Personal files | Partial | Private document store, metadata, share-in |
| MCP and integrations | Missing | Mobile capability broker; no arbitrary subprocess execution |
| Shell/Docker/workspaces | Replacement required | Sandboxed phone actions; optional explicit PC pairing only |
| Reminders/webhooks | Missing | WorkManager, notifications, optional signed webhook delivery |
| Auth/tokens/vault | Partial | Biometric lock and scoped integration-secret UI |
| Backup/preferences/themes | Partial | Restore validation, migrations, granular controls, themes |
| Security/diagnostics | Partial | Permission ledger, runtime health, data controls, diagnostics |

Desktop evidence: router registrations in `app.py` and user-facing modules in
`static/index.html` and `static/js/` from the desktop Odysseus source tree.
