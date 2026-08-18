# Mobdysseus — Full Native Android Parity Plan

Status: **Baseline green** (2026-08-18). Build + 111 unit tests + 7 contract tests pass;
59/59 desktop routes are classified across 28 MOB tickets; dependency boundaries pass.

This plan is the working contract for turning the current native shell into a complete,
standalone Android reconstruction of the desktop Odysseus app. It is grounded in
`PARITY.md`, `SWARM_BACKLOG.md`, and `docs/desktop-capability-inventory.json`.

---

## 1. Verified baseline (what "brought up" means)

| Check | Result |
|---|---|
| `gradlew.bat assembleDebug` (PC, authoritative) | BUILD SUCCESSFUL, `app-debug.apk` (74.7 MB) |
| `testDebugUnitTest` | 111 tests, 0 failures |
| `tools/` contract tests (`pytest`) | 7 passed |
| `validate_desktop_capability_inventory.py` | 59 entries, 59 routes covered, 28 tickets |
| `check_dependency_boundaries.py` | OK across 63 Kotlin files |
| Version control | Git repo initialised, baseline committed, `.gitignore` added, 807 MB heap dump removed |

Fixes landed to reach green:
- `platform/task/AndroidTaskReminderScheduler.kt` — corrupted import block (literal `\n`
  sequences) split into real imports.
- `core/task/AndroidTaskReminderStore.kt` — plaintext JSON codec extracted into
  `TaskReminderCodec` so it is JVM-testable; the store wraps it with Keystore encryption.
  The two failing codec tests now test the codec directly.

---

## 2. What is already native (implemented, not just shell)

- **App shell** — 6 destinations (Chat, Cookbook, Brain, Notes, Tasks, More), edge-to-edge,
  IME-safe, dark theme, encrypted workspace load/retry.
- **Persistence** — Room + SQLCipher + Android Keystore, v0 migration, encrypted backup codec,
  restore validation, diagnostics/cleanup.
- **Chat** — streaming local reply, local lexical retrieval context, dictation (offline STT),
  safe Markdown/code rendering, conversation export, draft preservation.
- **Cookbook** — recipe library, runs, schedules, model catalog with device-fit (LiteRT-LM),
  SHA-256 verification, SAF import.
- **Brain / Notes / Tasks** — memory, Markdown notes, task filters + WorkManager reminders.
- **More** — gallery + touch image editing, workspace search, settings, capability panel,
  provider panel, diagnostics, documents/RAG ingestion.
- **Core modules** — documents, memory, recipes, skills, tasks, voice, vault, calendar,
  contacts, image editing, retrieval, capability policy + audit ledger.

---

## 3. Desktop → native gap map (what is still missing)

Legend: `native` = on-device, `optional-network` = needs explicit consent/account,
`android-replacement` = permissioned Android capability, `impossible-standalone` = replaced/omitted.

### Wave 0 — foundation (mostly done, needs hardening)
- **MOB-001** Modular shell — DONE (boundary test green).
- **MOB-002** Typed encrypted data — DONE (Room + SQLCipher + Keystore + migration).
- **MOB-003** Capability broker + approval ledger — PARTIAL. Policy/audit exist; webhook
  delivery (`webhook_routes.py`) is `optional-network` and not wired.
- **MOB-004** S25 adaptive shell + accessibility — PARTIAL. IME-safe + dark theme exist;
  no landscape/multi-window, 48dp/talkback audit, or screenshot tests.

### Wave 1 — core product slices
- **MOB-010** Chat/history — PARTIAL. Streaming, retrieval, export exist; missing session
  rename/delete/search, attachments, retry/edit, single-chat export polish.
- **MOB-011** Model registry/storage — PARTIAL. Catalog + fit + SHA-256 exist; missing
  resumable foreground downloads, atomic verification, storage UI, active-model delete.
- **MOB-013** Personal files — PARTIAL. Private share-inbox + import policy exist; missing
  full document store metadata + share-in polish.
- **MOB-014** Notes — PARTIAL. Markdown + search exist; missing tags/folders, attachments.
- **MOB-015** Tasks/automation — PARTIAL. Filters + reminders exist; missing due-date
  recurrence UI, run history, notifications policy.
- **MOB-016** Voice — PARTIAL. Offline STT dictation exists; missing reliable offline STT
  engine, voice settings, voice routing, TTS.
- **MOB-017** Gallery/image editor — PARTIAL. Gallery + touch editing exist; missing
  camera/photo picker integration polish.
- **MOB-018** Calendar/contacts — PARTIAL. Android provider adapters exist; missing scoped
  runtime permission flows + UI.

### Wave 2 — intelligence
- **MOB-020** Cookbook execution — PARTIAL. Recipes/runs/schedules exist; missing compare,
  presets, output history.
- **MOB-021** Documents/RAG — PARTIAL. Ingestion + citations exist; missing full PDF/DOCX
  pipeline + citation UI.
- **MOB-022** Embeddings — PARTIAL. Local lexical index exists; missing on-device embedding
  model wiring.
- **MOB-023** Memory — PARTIAL. Semantic recall/edit/categories exist; missing import/export
  + extraction.
- **MOB-024** Assistant/agent — PARTIAL. Local chat exists; missing permissioned tool
  execution loop.
- **MOB-025** Skills/tools — PARTIAL. Skill library + verifier exist; missing permissioned
  Android capability registry + approval ledger UI.

### Wave 3 — trust/integrations (mostly optional-network)
- **MOB-030** Auth/backup/wipe — PARTIAL. Backup + wipe exist; missing biometric lock +
  scoped integration-secret UI.
- **MOB-031** Prefs/cleanup/diagnostics — PARTIAL. Diagnostics + cleanup exist; missing
  granular controls + font/theme.
- **MOB-032** Provider vault — PARTIAL. Keystore vault exists; missing provider adapters
  (API tokens, device flow, ChatGPT/Codex/Copilot) behind a hard local-only gate.
- **MOB-033** Web search/research — MISSING. Needs consented search, progress/cancel, safe
  URL open, cited report generation, offline archive.
- **MOB-034** Email — MISSING. Needs ACTION_SENDTO compose first; optional OAuth/IMAP
  inbox/search/read/attachments with encrypted tokens.
- **MOB-035** Desktop-only replacement guide — MISSING. Needs route→capability map + help.

### Wave 4 — ship gate
- **MOB-040** Automated parity/regression suite — PARTIAL. Unit + contract tests exist;
  missing Compose + screenshot tests, S25 config tests.
- **MOB-041** Physical S25 verification/soak — NOT STARTED (HITL).
- **MOB-042** Signed release APK + handoff — NOT STARTED (HITL for signing key).

---

## 4. Execution plan (phased, dependency-ordered)

### Phase A — Harden the foundation (MOB-003, MOB-004)
1. Wire webhook delivery as a permissioned, signed, optional capability (MOB-003).
2. S25 adaptive shell: landscape/multi-window, 48dp touch targets, TalkBack semantics,
   screenshot tests for portrait/landscape/large-text (MOB-004).

### Phase B — Complete core product slices (MOB-010, 011, 013, 014, 015, 016, 017, 018)
3. Chat: session rename/delete/search, attachments, retry/edit, single-chat export.
4. Model manager: resumable foreground downloads, atomic verify, storage UI, delete.
5. Notes: tags/folders, attachments. Tasks: recurrence UI, run history, notification policy.
6. Voice: reliable offline STT engine + settings + routing + TTS.
7. Gallery: camera/photo picker polish. Calendar/contacts: scoped permission flows + UI.

### Phase C — Intelligence (MOB-020, 021, 022, 023, 024, 025)
8. Cookbook: compare, presets, output history.
9. Documents/RAG: full PDF/DOCX pipeline + citation UI.
10. Embeddings: on-device embedding model wiring.
11. Memory: import/export + extraction.
12. Assistant: permissioned tool-execution loop. Skills: capability registry + approval UI.

### Phase D — Trust/integrations (MOB-030, 031, 032, 033, 034, 035)
13. Biometric lock + scoped integration-secret UI; granular prefs/theme/font.
14. Provider vault adapters behind hard local-only gate.
15. Web search/research with consent, progress, citations, offline archive.
16. Email compose (ACTION_SENDTO) + optional OAuth/IMAP.
17. Desktop-only replacement guide + help.

### Phase E — Ship gate (MOB-040, 041, 042)
18. Compose + screenshot + S25 config tests; release lint clean.
19. Physical S25 verification/soak (HITL).
20. Signed release APK, checksum, install guide (HITL for signing key).

---

## 5. Definition of shipped (from SWARM_BACKLOG)

- All standalone-core tickets through MOB-042 pass.
- Release APK is signed and upgrade-tested; S25 verification matrix has no critical failures.
- Network integrations are optional; local-only mode works in airplane mode.
- Desktop shell/Docker/host-filesystem/browser-automation are represented by permissioned
  Android capabilities — never hidden PC/Pi dependencies.

---

## 6. Key decisions / risks

- **No WebView** — the project contract forbids it; the Capacitor `llmfullload-apk-staging`
  wrapper is out of scope for the native app.
- **Version control** — Pi copy is now the git source of truth; PC is the build host. Sync
  source only (exclude `.gradle`, `.kotlin`, `app/build`, `local.properties`, heap dumps,
  wrapper JAR).
- **HITL items** — physical S25 sign-off, signing-key ownership, account/OAuth consent,
  consequential-action approvals. These cannot be completed unattended.
- **Optional-network features** (search, email, providers) are gated behind explicit consent
  and a hard local-only switch; they must never break airplane-mode operation.
