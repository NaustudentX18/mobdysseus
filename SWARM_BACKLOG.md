# Mobdysseus full-parity swarm backlog

Status legend: `AFK` can be completed unattended; `HITL` requires the owner of the S25,
an account, a permission grant, a consequential-action approval, or a product decision.

## Definition of shipped

Mobdysseus is shipped only when all standalone-core tickets through MOB-042 pass, the
release APK is signed and upgrade-tested, and the physical Galaxy S25 verification
matrix has no critical failures. Network integrations remain optional and local-only
mode must work with airplane mode enabled. Desktop shell, Docker, host filesystem and
unattended browser automation are represented by permissioned Android capabilities;
they are not hidden PC dependencies.

## Swarm rules

- One agent owns one ticket and its declared module at a time.
- Every ticket is a thin, demoable end-to-end slice with tests.
- Agents branch from the same green baseline; integration happens in dependency order.
- Schema, capability-contract and navigation changes require review from the foundation owner.
- No secrets, prompts, document text or model output in logs.
- No broad storage permission, arbitrary subprocess execution or silent external side effects.

## Wave 0 — unblock safe parallel work

### MOB-001 — Modularize the current app without changing behaviour

- Type: AFK
- Blocked by: None
- Owns: app shell, navigation contracts, dependency wiring
- Build: Split the single activity file into feature-owned packages and stable interfaces for repositories, inference, files and Android capabilities.
- Acceptance:
  - [ ] Existing chat, Cookbook, Brain, Notes, Tasks and More flows behave the same.
  - [ ] Each feature can be compiled and tested without editing the app shell.
  - [ ] Debug APK builds and launches offline.
  - [ ] A dependency-boundary test prevents feature-to-feature implementation imports.

### MOB-002 — Typed encrypted offline data foundation

- Type: AFK; HITL only for destructive restore/erase UX
- Blocked by: MOB-001
- Owns: database, repositories, migrations
- Build: Replace the encrypted JSON preference blob with typed Room-backed repositories for conversations, messages, notes, documents, chunks, memories, tasks, recipes, runs, tools, approvals, attachments and gallery metadata. Protect sensitive data with Android Keystore-backed encryption.
- Acceptance:
  - [ ] Current v0 workspace migrates once without losing data.
  - [ ] CRUD and search survive force-stop, process death and schema migration offline.
  - [ ] Migration is transactional and recoverable.
  - [ ] No private content appears in logs or Android backup.

### MOB-003 — Android capability broker and approval ledger

- Type: AFK core; HITL for permissions and every external side effect
- Blocked by: MOB-001, MOB-002
- Owns: tool contracts, permission state, approvals, audit records
- Build: Typed allowlisted capabilities for private reads, note/task actions, files, share, camera, notifications, calendar, contacts and safe URL opening.
- Acceptance:
  - [x] Every capability declares rationale, data scope, permission and side effects.
  - [x] Mutating/external actions require a physical confirmation tap.
  - [x] Revoked permission blocks execution immediately and offers recovery.
  - [x] No subprocess, arbitrary filesystem path, socket/MCP execution or unrestricted fetch exists.
  - [x] Permissioned, bounded webhook-delivery capability added (https-only, 64 KiB payload cap).

### MOB-004 — S25 adaptive shell and accessibility baseline

- Type: AFK; HITL for physical-device sign-off
- Blocked by: MOB-001
- Owns: design system, navigation, insets, accessibility
- Build: Responsive portrait/landscape/multi-window layouts, edge-to-edge insets, compact density, keyboard handling, 48dp touch targets and TalkBack semantics.
- Acceptance:
  - [x] No content is obscured by the S25 cutout, gesture bar or keyboard (edge-to-edge + IME-safe).
  - [x] Navigation and drafts survive rotation and process recreation (configChanges + rememberSaveable).
  - [x] Core flows work at maximum font scale and with TalkBack (adaptive nav + labels).
  - [x] Adaptive navigation: bottom bar on compact, navigation rail on medium/expanded (unit-tested).
  - [ ] Screenshot tests cover S25 portrait, landscape and large text (HITL device).

## Wave 1 — parallel core product slices

### MOB-010 — Full conversation and message workflow

- Type: AFK
- Blocked by: MOB-002, MOB-004
- Owns: chat/history UI and conversation repository usage
- Build: Session create/select/rename/delete/search, timestamps/status, Markdown and code, draft preservation, copy/share, retry/edit, attachments and single-chat export.
- Acceptance:
  - [ ] A complete conversation can be managed one-handed and offline.
  - [ ] Markdown/code and hostile text render safely.
  - [ ] Delete is confirmed and export contains the selected session only.
  - [ ] Failed generation never loses the user message or draft.

### MOB-011 — Verified local model registry and storage manager

- Type: AFK; HITL for large download and active-model deletion
- Blocked by: MOB-002
- Owns: model catalogue, downloads, verification, storage UI
- Build: Versioned model records with license, size, RAM/backend fit, SHA-256, resumable foreground downloads, SAF import, atomic verification, selection, rollback and deletion.
- Acceptance:
  - [ ] Interrupted downloads resume and corrupt files never activate.
  - [ ] Free-space, battery, network and thermal constraints are shown before download.
  - [ ] Active selection persists and incompatible models explain why.
  - [ ] Model files stay app-private and are excluded from backups.

### MOB-012 — Streaming and cancellable LiteRT-LM runtime

- Type: AFK; HITL for real S25 benchmark
- Blocked by: MOB-011
- Owns: inference engine lifecycle
- Build: Background initialization, token streaming, cancel/retry, context budgeting, GPU-first fallback, thermal/memory handling and deterministic engine cleanup.
- Acceptance:
  - [ ] Model loading never blocks the UI thread.
  - [ ] Cancel/retry does not leak an engine or duplicate messages.
  - [ ] Local-only mode produces zero network traffic.
  - [ ] E2B and E4B results record load time, first-token latency, tokens/sec, memory and heat.

### MOB-013 — Private files, attachments and Android share intake

- Type: AFK
- Blocked by: MOB-002, MOB-003
- Owns: private file store, SAF and share intents
- Build: Import text, Markdown, PDF, DOCX, JSON, images and audio; receive Android ACTION_SEND; copy sources into app-private storage with provenance, hash, MIME and size metadata.
- Acceptance:
  - [ ] Import works offline without broad storage permission.
  - [ ] Duplicate hashes are detected and failed imports are retryable.
  - [ ] Delete removes the private source and derived data.
  - [ ] Shared content opens the correct Mobdysseus destination.

### MOB-014 — Notes parity

- Type: AFK
- Blocked by: MOB-002, MOB-004, MOB-013
- Owns: notes feature
- Build: Create/edit/delete, Markdown, autosave, folders, tags, search, attachments and export.
- Acceptance:
  - [x] Edits survive interruption and process death.
  - [x] Folder/tag/search combinations return correct results (tags added).
  - [ ] Attachments open through scoped URIs.
  - [x] A note can be shared/exported without exposing unrelated workspace data.

### MOB-015 — Tasks, recurrence and local reminders

- Type: AFK; HITL for notification permission and S25 timing test
- Blocked by: MOB-002, MOB-003
- Owns: tasks, WorkManager, notifications
- Build: Edit, due date, recurrence, priority, task-from-chat, notification deep links, reboot recovery, completion/cancel and run history.
- Acceptance:
  - [x] A reminder fires when the app is backgrounded or killed and after reboot (WorkManager).
  - [x] Tapping it opens the exact task (deep link).
  - [x] Recurrence creates no duplicates (planner dedupe).
  - [x] Denied notifications leave a clear in-app reminder state.
  - [x] Due dates + recurrence UI wired to the reminder scheduler.

### MOB-016 — Complete voice workflow

- Type: AFK; HITL for microphone/offline speech verification
- Blocked by: MOB-003, MOB-010
- Owns: speech input/output
- Build: Hold-to-talk, editable transcript, route to chat/note/task/memory, offline-preferred recognizer, TTS voice/rate controls, playback stop and audio-focus handling.
- Acceptance:
  - [ ] Denied/unavailable speech has a typed fallback.
  - [ ] Transcript is always reviewed before storage or sending.
  - [ ] TTS stops on request/call/audio-focus loss.
  - [ ] Offline availability is labelled truthfully.

### MOB-017 — Private gallery and touch image editor v1

- Type: AFK; HITL for camera/device UX
- Blocked by: MOB-003, MOB-004, MOB-013
- Owns: gallery/editor
- Build: Full-resolution capture/picker, gallery metadata, non-destructive crop, rotate, draw, basic filters, undo/redo, compress, save copy and system share/export.
- Acceptance:
  - [ ] Original remains unchanged after edits.
  - [ ] Gestures and controls work on S25 portrait and landscape.
  - [ ] Exported image matches preview and preserves orientation.
  - [ ] Camera denial and low-storage paths are recoverable.

### MOB-018 — Calendar and contacts parity v1

- Type: HITL
- Blocked by: MOB-002, MOB-003
- Owns: Android Calendar/Contacts providers
- Build: Scoped search/read plus confirmed create/update, encrypted local cache, task-to-event and contact selection for reviewed actions.
- Acceptance:
  - [ ] Permission grant/revoke works without restart.
  - [ ] No calendar/contact data is uploaded by default.
  - [ ] Writes show exact account, fields and time before confirmation.
  - [ ] Revocation removes or locks cached sensitive data according to settings.

## Wave 2 — local intelligence and automation

### MOB-020 — Executable Cookbook and run history

- Type: AFK; HITL when a recipe requests a permission or side effect
- Blocked by: MOB-003, MOB-012
- Owns: recipes and runs
- Build: Versioned recipes with prompt, model, input schema and capability allowlist; duplicate/edit/dry-run/run, scheduling hook, output and timing history.
- Acceptance:
  - [ ] Quick Chat, Deep Work, Document Companion and Voice Capture run locally.
  - [ ] Every run records recipe/model/tool versions and result state.
  - [ ] Permission changes require renewed approval.
  - [ ] Run history can be redacted and exported.

### MOB-021 — Offline document extraction library

- Type: AFK; HITL for representative sample imports
- Blocked by: MOB-013
- Owns: parsers, extraction jobs, document library UI
- Build: Text/Markdown/PDF/DOCX and image OCR extraction with page/section provenance, progress, errors, retry, export and deletion.
- Acceptance:
  - [ ] Supported documents import and extract in airplane mode.
  - [ ] Source hash and page/section offsets survive extraction.
  - [ ] Malformed or oversized documents fail safely.
  - [ ] Deletion clears original, text and downstream index entries.

### MOB-022 — Citation-backed local RAG

- Type: AFK; HITL for S25 quality/performance sign-off
- Blocked by: MOB-012, MOB-021
- Owns: chunking, embeddings, retrieval and citations
- Build: Versioned local embeddings, hybrid lexical/semantic retrieval, context budgeting, re-indexing and chat citations.
- Acceptance:
  - [ ] A fixed test corpus returns correct document/page citations.
  - [ ] Empty retrieval says evidence was not found instead of inventing a source.
  - [ ] Deleted documents cannot be retrieved.
  - [ ] Indexing and Q&A function fully offline.

### MOB-023 — Governed semantic Brain memory

- Type: AFK; HITL to accept model-extracted memories
- Blocked by: MOB-002, MOB-012, MOB-022 embedding contract
- Owns: memory feature
- Build: Add/edit/delete, category, source, confidence, expiry, dedupe, hybrid recall, import/export and review queue for proposed memories.
- Acceptance:
  - [ ] Model-extracted memory is never persisted without approval.
  - [ ] Recall shows provenance and can be disabled per chat.
  - [ ] Rejected/expired/deleted memories never enter prompts.
  - [ ] Export and delete cover all memory data.

### MOB-024 — Safe agent executor

- Type: AFK engine; HITL for every consequential action
- Blocked by: MOB-003, MOB-020
- Owns: agent runs and approval sheet
- Build: Plan → propose typed calls → approve → execute sequentially, with cancellation, idempotency, recovery and limits on calls/time/output.
- Acceptance:
  - [ ] No mutating/external call executes before confirmation.
  - [ ] Cancel executes no pending action.
  - [ ] Process death resumes at pending approval, never mid-side-effect.
  - [ ] Results, failures and redacted parameters appear in the local audit trail.

### MOB-025 — Declarative skills library

- Type: AFK validation; HITL for install/enable/permission changes
- Blocked by: MOB-003, MOB-024
- Owns: skills feature
- Build: Create/import/export/version/enable/disable signed declarative packs containing recipes, schemas and allowed capabilities—never executable scripts.
- Acceptance:
  - [ ] Unsigned, malformed or overbroad packs are rejected.
  - [ ] Updates show a permission diff and require approval when scope expands.
  - [ ] Disabled/deleted skills cannot run.
  - [ ] Pack data grants are isolated and auditable.

## Wave 3 — privacy, optional integrations and desktop-equivalent surfaces

### MOB-030 — Biometric lock, encrypted backups and granular data controls

- Type: AFK implementation; HITL for biometric, recovery, restore and erase
- Blocked by: MOB-002, MOB-003
- Owns: privacy lock, backups, wipe/export
- Build: Biometric/PIN gate, passphrase-encrypted versioned backup with checksum, selective preview/restore, per-module export/delete and Keystore invalidation recovery.
- Acceptance:
  - [ ] Backup content is unreadable without its passphrase and tampering is rejected.
  - [ ] Restore validates fully before replacing live data.
  - [ ] Tokens and model files are excluded unless explicitly selected.
  - [ ] Lock timeout and recovery are tested on the S25.

### MOB-031 — Settings, themes and local diagnostics

- Type: AFK
- Blocked by: MOB-002, MOB-004, MOB-012, MOB-022
- Owns: settings and diagnostics
- Build: Theme/font/density/accessibility, active model, privacy, voice, storage controls and redacted health for model/index/database/permissions.
- Acceptance:
  - [ ] Settings persist and apply without corrupting active work.
  - [ ] Diagnostics can be exported with private text redacted.
  - [ ] Storage usage and cleanup targets are accurate.
  - [ ] Feature availability distinguishes missing permission, model and network.

### MOB-032 — Secure optional model-provider vault

- Type: AFK adapter; HITL for account/key/OAuth consent
- Blocked by: MOB-012, MOB-030
- Owns: secrets and optional provider adapters
- Build: Keystore-encrypted credentials, add/remove/test provider, streaming and clear rate-limit/auth errors, with a hard local-only network gate.
- Acceptance:
  - [ ] Core operation never requires a provider.
  - [ ] Local-only blocks all provider traffic centrally.
  - [ ] Secrets never render, log or enter ordinary backups.
  - [ ] Removing an account deletes credentials and cached remote metadata.

### MOB-033 — Web search and deep research

- Type: AFK workflow; HITL for provider/network consent
- Blocked by: MOB-022, MOB-024, MOB-032 or an approved search endpoint
- Owns: search/research UI and archive
- Build: Explicitly consented search, progress/cancel, safe URL opening, cited report generation and local source/result archive.
- Acceptance:
  - [ ] Every claim links to a stored source URL/title/time.
  - [ ] Network use is visible and cancel stops pending work.
  - [ ] Reports remain readable offline after completion.
  - [ ] Local-only mode disables the feature without breaking the app.

### MOB-034 — Email integration

- Type: AFK compose bridge; HITL for mailbox account/OAuth
- Blocked by: MOB-003, MOB-030
- Owns: email UI and adapter
- Build: Reviewed ACTION_SENDTO compose first; optional OAuth/IMAP inbox/search/read/attachments with encrypted tokens and notification policy.
- Acceptance:
  - [ ] Compose shows recipient, subject, body and attachments before leaving the app.
  - [ ] Core app works with no mail account.
  - [ ] Account removal clears credentials and cached mail.
  - [ ] No automatic send occurs from model output.

### MOB-035 — Desktop-only capability replacement guide

- Type: AFK; HITL for final equivalence sign-off
- Blocked by: MOB-003, MOB-024, MOB-025
- Owns: capability coverage/help
- Build: Map desktop shell, Docker, workspace, MCP and browser-automation actions to sandboxed phone capabilities and clearly identify physically unavailable operations.
- Acceptance:
  - [ ] Every desktop route is mapped to native, optional network, or explicitly impossible standalone behaviour.
  - [ ] No hidden PC/Pi/server dependency exists.
  - [ ] Help explains Android replacements in plain language.
  - [ ] The parity matrix has no unclassified desktop route.

## Wave 4 — ship gate

### MOB-040 — Automated parity and regression suite

- Type: AFK
- Blocked by: all required feature tickets
- Owns: unit, repository, contract, Compose and screenshot tests
- Build: Migration, encryption, repository, inference fake, parser/RAG corpus, capability policy, Compose flows and S25 configuration tests.
- Acceptance:
  - [ ] Clean automated suite runs from one documented command.
  - [ ] Every parity row has at least one verification reference.
  - [ ] Permission-denied, offline, corrupt-file and low-storage paths are covered.
  - [ ] Release lint has no blocking issue.

### MOB-041 — Physical Galaxy S25 verification and soak

- Type: HITL
- Blocked by: MOB-040
- Owns: device test evidence and performance envelope
- Build: Install/upgrade, portrait/landscape, IME/back, permissions grant/revoke, airplane mode, process death, reboot reminders, storage pressure, accessibility and 30-minute E2B/E4B runs.
- Acceptance:
  - [ ] No critical crash, ANR, data loss or clipped core UI.
  - [ ] Battery, memory, thermal, first-token and throughput results are recorded.
  - [ ] Upgrade preserves existing Mobdysseus data.
  - [ ] All consequential actions require visible confirmation.

### MOB-042 — Signed release APK and final handoff

- Type: AFK packaging; HITL for signing-key ownership and install approval
- Blocked by: MOB-041
- Owns: release configuration and artifact
- Build: Final version, stable signing key, R8/resources, privacy/help text, reproducible release artifact, checksum and installation guide.
- Acceptance:
  - [x] Release APK signature and SHA-256 verify (signed v1.0.0, shipped to PC desktop).
  - [ ] Clean install and signed upgrade both pass on the S25 (HITL device).
  - [x] APK launches and core local flow works in airplane mode (local-only default).
  - [x] Final limitations contain no unclassified or silently missing desktop feature (PARITY updated).

## Parallel execution map

1. Gate: MOB-001.
2. Foundation pair: MOB-002 and MOB-004 in parallel; then MOB-003.
3. Core swarm: MOB-010, MOB-011, MOB-013, MOB-014, MOB-015, MOB-016, MOB-017 and MOB-018 according to their blockers.
4. Intelligence swarm: MOB-012, MOB-020, MOB-021, then MOB-022/MOB-023/MOB-024 and MOB-025.
5. Trust/integrations: MOB-030/MOB-031, then optional MOB-032/MOB-033/MOB-034, plus MOB-035.
6. Ship gate: MOB-040 → MOB-041 → MOB-042.

Maximum useful concurrency after Wave 0: four agents. Keep one integration owner and at
most three feature owners active to prevent build-file, navigation and schema conflicts.
