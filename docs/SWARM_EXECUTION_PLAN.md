# Mobdysseus — Swarm Execution Plan (take-out pack)

Self-contained work orders for a parallel agent swarm to complete the native Android
reconstruction of the desktop Odysseus app. Agents pick up one ticket, follow the operating
rules, and integrate against the green baseline. No prior conversation is required.

Companion docs: `AGENTS.md` (contract), `PARITY.md` (parity matrix), `SWARM_BACKLOG.md`
(backlog), `docs/FULL_PARITY_PLAN.md` (strategy), `docs/desktop-capability-inventory.json`
(route coverage).

---

## 0. Mission

Build **Mobdysseus**: a native, standalone Android (Kotlin/Compose) app that reproduces the
desktop Odysseus workspace for personal use. **No WebView.** Must work fully offline in
airplane mode with no PC, Pi, API key, or server. Desktop-only actions get a permissioned
Android equivalent — never a hidden remote dependency.

## 1. Green baseline (verified 2026-08-18)

- `assembleDebug` → BUILD SUCCESSFUL, `app-debug.apk` (74.7 MB).
- `testDebugUnitTest` → 111 tests, 0 failures.
- `tools/` contract tests (`pytest`) → 7 passed.
- `validate_desktop_capability_inventory.py` → 59 entries, 59 routes, 28 tickets.
- `check_dependency_boundaries.py` → OK across 63 Kotlin files.
- Git repo initialised on the Pi (source of truth); baseline committed.

## 2. Operating rules (non-negotiable)

1. **One agent, one ticket.** Claim a ticket and its declared module before editing.
2. **Branch from the green baseline.** Never build on another agent's unmerged work.
3. **Thin, demoable, tested slices.** Every ticket ships an end-to-end slice with tests.
4. **Shared-contract review.** Schema, capability-contract, and navigation changes require
   review from the foundation owner (MOB-001/002/003) before merge.
5. **No cross-feature imports.** Feature A must not import feature B's implementation.
6. **Privacy & safety.** No secrets, prompts, document text, or model output in logs. No
   broad storage permission, arbitrary subprocess, unrestricted filesystem path, or silent
   external side effect. External writes/sharing/sending require visible user confirmation.
7. **Concurrency cap.** After Wave 0, at most **4 agents**: 1 integration owner + 3 feature
   owners. Prevents build-file, navigation, and schema conflicts.
8. **Do not touch BobTheBuilder** or unrelated projects.

## 3. Build, sync, and verify (authoritative host = Windows PC)

The Android build runs on the paired Windows PC (`pc`). The Pi copy is the git source of truth.

```bash
# Sync source from Pi to PC (exclude build artifacts):
#   .gradle, .kotlin, app/build, local.properties, *.hprof, gradle/wrapper/gradle-wrapper.jar
scp -r -F ~/.ssh/config app pc:C:/Users/jakem/Projects/odysseus-mobile-native/

# Build (PC):
ssh -F ~/.ssh/config pc 'cd C:\Users\jakem\Projects\odysseus-mobile-native && gradlew.bat --no-daemon assembleDebug'

# Unit tests (PC):
ssh -F ~/.ssh/config pc 'cd C:\Users\jakem\Projects\odysseus-mobile-native && gradlew.bat --no-daemon testDebugUnitTest'

# Contract tests (Pi):
python3 -m pytest tools/ -q
python3 tools/validate_desktop_capability_inventory.py
python3 tools/check_dependency_boundaries.py
```

**Definition of done for every ticket:** green compile + ticket-proportionate tests on the PC,
no new lint blockers, no private content in logs, and the ticket's acceptance boxes checked.

---

## 4. Work orders

Legend — **Status**: `DONE` / `PARTIAL` / `MISSING` / `NOT STARTED`. **Type**: `AFK`
(unattended) / `HITL` (needs S25, account, permission, or product decision).

### Wave 0 — Foundation

#### MOB-001 — Modular app shell
- Status: **DONE** · Type: AFK · Blocked by: none
- Owns: `MainActivity.kt`, `navigation/Destination.kt`, dependency wiring.
- Evidence: `tools/check_dependency_boundaries.py` green (63 files).
- DoD: boundary test stays green; no shell edits without foundation-owner review.

#### MOB-002 — Typed encrypted offline data
- Status: **DONE** · Type: AFK · Blocked by: MOB-001
- Owns: `persistence/**`, `core/Workspace.kt`, `core/WorkspaceStore.kt`.
- Evidence: Room + SQLCipher + Keystore, v0 migration, `V0RoomMigrationCoordinatorTest`,
  `CoreWorkspaceMapperTest`, `EncryptedBackupCodecTest` green.
- DoD: migration/CRUD/search survive force-stop, process death, schema migration offline.

#### MOB-003 — Capability broker + approval ledger
- Status: **PARTIAL** · Type: AFK core (HITL for permissions/side effects) · Blocked by: MOB-001, MOB-002
- Owns: `capability/CapabilityPolicy.kt`, `capability/CapabilityAuditPersistence.kt`,
  `feature/CapabilityPanel.kt`.
- Current: typed allowlist + audit + panel exist. **Gap:** webhook delivery
  (`webhook_routes.py`, optional-network) is not wired as a permissioned capability.
- Build: add a signed, permissioned webhook-delivery capability behind the local-only gate;
  every capability declares rationale/data-scope/permission/side-effects; mutating/external
  actions require a physical confirmation tap; revoked permission blocks immediately.
- Acceptance: capability contract tests + audit-ledger tests green; no subprocess/arbitrary
  path/socket/MCP/unrestricted fetch.

#### MOB-004 — S25 adaptive shell + accessibility
- Status: **PARTIAL** · Type: AFK (HITL device sign-off) · Blocked by: MOB-001
- Owns: `MainActivity.kt`, `ui/S25Accessibility.kt`, `navigation/Destination.kt`.
- Current: edge-to-edge + IME-safe + dark theme exist.
- **Gap:** landscape/multi-window, 48dp touch targets, TalkBack semantics, screenshot tests
  (portrait/landscape/large-text).
- Build: responsive layouts, insets, compact density, keyboard handling, TalkBack semantics;
  add Compose screenshot tests for S25 configs.
- Acceptance: no content obscured by cutout/gesture-bar/keyboard; nav + drafts survive
  rotation/recreation; core flows work at max font scale + TalkBack.

### Wave 1 — Core product slices

#### MOB-010 — Full conversation workflow
- Status: **PARTIAL** · Type: AFK · Blocked by: MOB-002, MOB-004
- Owns: `feature/ChatScreen.kt`, `feature/ChatWorkflow.kt`, `feature/StreamingTextAssembler.kt`,
  `feature/SafeMarkdownBlocks.kt`.
- Current: streaming, local retrieval, dictation, safe Markdown, export, draft preservation.
- **Gap:** session create/select/rename/delete/search, timestamps/status, attachments,
  retry/edit, single-chat export polish.
- Acceptance: one-handed offline session management; hostile text renders safely; delete is
  confirmed; export contains only the selected session; failed generation never loses the
  user message/draft.

#### MOB-011 — Verified local model registry + storage manager
- Status: **PARTIAL** · Type: AFK (HITL large download/delete) · Blocked by: MOB-002
- Owns: `model/ModelDomain.kt`, `model/ModelArtifactStore.kt`, `model/ModelImportPolicy.kt`,
  `model/S25ModelCatalog.kt`, `model/Sha256.kt`, `model/AndroidDeviceProfileProbe.kt`,
  `feature/CookbookScreen.kt`.
- Current: catalog + device-fit + SHA-256 + SAF import exist.
- **Gap:** resumable foreground downloads, atomic verification, storage UI, active-model
  delete, free-space/battery/network/thermal pre-checks.
- Acceptance: corrupt/incomplete files never activate; constraints shown before download;
  active selection persists; model files app-private + excluded from backups.

#### MOB-012 — Streaming cancellable LiteRT-LM runtime
- Status: **PARTIAL** · Type: AFK (HITL S25 benchmark) · Blocked by: MOB-011
- Owns: `LocalModelRuntime.kt`, `feature/ChatScreen.kt` (inference path).
- Current: `LocalModelRuntime` exists; chat uses a native fallback reply.
- **Gap:** background init, token streaming, cancel/retry, context budgeting, GPU-first
  fallback, thermal/memory handling, deterministic cleanup, E2B/E4B benchmark.
- Acceptance: loading never blocks UI; cancel/retry leaks no engine/duplicates; local-only
  produces zero network traffic; benchmark records load/first-token/tokens-per-sec/memory/heat.

#### MOB-013 — Private files, attachments, share intake
- Status: **PARTIAL** · Type: AFK · Blocked by: MOB-002, MOB-003
- Owns: `core/file/PrivateShareInbox.kt`, `core/file/ShareImportPolicy.kt`,
  `feature/DocumentsPanel.kt`.
- Current: private share-inbox + import policy exist.
- **Gap:** full import of text/Markdown/PDF/DOCX/JSON/images/audio; ACTION_SEND receiver;
  provenance/hash/MIME/size metadata; duplicate-hash detection; retryable failures.
- Acceptance: import offline without broad storage permission; delete removes source +
  derived data; shared content opens the correct destination.

#### MOB-014 — Notes parity
- Status: **PARTIAL** · Type: AFK · Blocked by: MOB-002, MOB-004, MOB-013
- Owns: `feature/WorkspaceFeatureScreens.kt` (NotesScreen), `feature/NoteFeatureSupport.kt`.
- Current: create/edit/delete, Markdown, search.
- **Gap:** autosave, folders, tags, attachments, export.
- Acceptance: edits survive interruption/process death; folder/tag/search combos correct;
  attachments open via scoped URIs; share/export exposes only the selected note.

#### MOB-015 — Tasks, recurrence, local reminders
- Status: **PARTIAL** · Type: AFK (HITL notification permission/timing) · Blocked by: MOB-002, MOB-003
- Owns: `core/task/TaskSchedule.kt`, `core/task/AndroidTaskReminderStore.kt`,
  `platform/task/AndroidTaskReminderScheduler.kt`, `feature/WorkspaceFeatureScreens.kt`
  (TasksScreen).
- Current: filters + WorkManager reminders + codec tests green.
- **Gap:** due-date/recurrence UI, priority, task-from-chat, notification deep links, reboot
  recovery, completion/cancel, run history.
- Acceptance: reminder fires backgrounded/killed + after reboot; tap opens exact task;
  recurrence creates no duplicates; denied notifications leave clear in-app state.

#### MOB-016 — Complete voice workflow
- Status: **PARTIAL** · Type: AFK (HITL mic/offline speech) · Blocked by: MOB-003, MOB-010
- Owns: `core/voice/AndroidVoiceSupport.kt`, `core/voice/VoicePolicy.kt`,
  `feature/ChatScreen.kt` (dictation).
- Current: offline-preferred dictation exists.
- **Gap:** hold-to-talk, editable transcript, route to chat/note/task/memory, TTS
  voice/rate controls, playback stop, audio-focus handling.
- Acceptance: denied/unavailable speech has typed fallback; transcript reviewed before
  storage/send; TTS stops on request/call/focus-loss; offline availability labelled truthfully.

#### MOB-017 — Private gallery + touch image editor v1
- Status: **PARTIAL** · Type: AFK (HITL camera/device UX) · Blocked by: MOB-003, MOB-004, MOB-013
- Owns: `core/image/ImageEditRecipe.kt`, `feature/MoreScreen.kt` (gallery/editor).
- Current: gallery + touch editing exist.
- **Gap:** full-res capture/picker, gallery metadata, non-destructive crop/rotate/draw/
  filters, undo/redo, compress, save copy, system share/export.
- Acceptance: original unchanged after edits; gestures work portrait/landscape; exported
  image matches preview + orientation; camera-denial/low-storage recoverable.

#### MOB-018 — Calendar + contacts parity v1
- Status: **PARTIAL** · Type: HITL · Blocked by: MOB-002, MOB-003
- Owns: `AndroidCalendar.kt`, `AndroidContacts.kt`.
- Current: provider adapters exist.
- **Gap:** scoped search/read + confirmed create/update, encrypted local cache,
  task-to-event, contact selection for reviewed actions, permission grant/revoke without
  restart.
- Acceptance: no calendar/contact data uploaded by default; writes show exact account/fields/
  time before confirmation; revocation removes/locks cached data per settings.

### Wave 2 — Local intelligence & automation

#### MOB-020 — Executable Cookbook + run history
- Status: **PARTIAL** · Type: AFK (HITL when recipe requests permission) · Blocked by: MOB-003, MOB-012
- Owns: `core/recipe/RecipeDomain.kt`, `core/recipe/RecipeRun.kt`, `feature/CookbookScreen.kt`.
- Current: recipes/runs/schedules exist.
- **Gap:** versioned recipes with prompt/model/input-schema/capability-allowlist;
  duplicate/edit/dry-run/run; scheduling hook; output + timing history; redacted export.
- Acceptance: Quick Chat / Deep Work / Document Companion / Voice Capture run locally; every
  run records recipe/model/tool versions + result state; permission changes require renewed
  approval.

#### MOB-021 — Offline document extraction library
- Status: **PARTIAL** · Type: AFK (HITL sample imports) · Blocked by: MOB-013
- Owns: `core/document/DocumentIngestion.kt`, `core/document/DocumentLifecycle.kt`,
  `feature/DocumentsPanel.kt`.
- Current: ingestion + lifecycle exist.
- **Gap:** text/Markdown/PDF/DOCX + image OCR with page/section provenance, progress,
  errors, retry, export, deletion.
- Acceptance: supported docs import/extract in airplane mode; source hash + page/section
  offsets survive; malformed/oversized fail safely; deletion clears original + text + index.

#### MOB-022 — Citation-backed local RAG
- Status: **PARTIAL** · Type: AFK (HITL S25 quality sign-off) · Blocked by: MOB-012, MOB-021
- Owns: `core/retrieval/LocalLexicalIndex.kt`, `feature/ChatScreen.kt` (retrieval context).
- Current: lexical index + retrieval context exist.
- **Gap:** versioned local embeddings, hybrid lexical/semantic retrieval, context budgeting,
  re-indexing, chat citations.
- Acceptance: fixed corpus returns correct document/page citations; empty retrieval says
  "evidence not found" (never invents a source); deleted docs not retrievable; fully offline.

#### MOB-023 — Governed semantic Brain memory
- Status: **PARTIAL** · Type: AFK (HITL to accept extracted memories) · Blocked by: MOB-002, MOB-012, MOB-022
- Owns: `core/memory/MemoryFeatureSupport.kt`, `core/memory/MemoryGovernance.kt`,
  `feature/WorkspaceFeatureScreens.kt` (BrainScreen).
- Current: memory + governance exist.
- **Gap:** category/source/confidence/expiry/dedupe, hybrid recall, import/export, review
  queue for proposed memories.
- Acceptance: model-extracted memory never persisted without approval; recall shows
  provenance + can be disabled per chat; rejected/expired/deleted memories never enter
  prompts; export/delete cover all memory data.

#### MOB-024 — Safe agent executor
- Status: **MISSING** · Type: AFK engine (HITL every consequential action) · Blocked by: MOB-003, MOB-020
- Owns: new `core/agent/` package + approval sheet UI.
- Build: plan → propose typed calls → approve → execute sequentially; cancellation,
  idempotency, recovery, limits on calls/time/output.
- Acceptance: no mutating/external call before confirmation; cancel executes no pending
  action; process death resumes at pending approval (never mid-side-effect); results/
  failures/redacted params in local audit trail.

#### MOB-025 — Declarative skills library
- Status: **PARTIAL** · Type: AFK validation (HITL install/enable/permission) · Blocked by: MOB-003, MOB-024
- Owns: `core/skills/SkillPackDomain.kt`, `core/skills/SkillPackVerifier.kt`,
  `core/skills/SkillPackCanonicalizer.kt`, `core/skills/SkillLibraryReducer.kt`.
- Current: skill library + verifier + canonicalizer exist.
- **Gap:** create/import/export/version/enable/disable signed declarative packs (recipes +
  schemas + allowed capabilities, never executable scripts); permission-diff on update.
- Acceptance: unsigned/malformed/overbroad packs rejected; updates show permission diff +
  require approval when scope expands; disabled/deleted skills cannot run; grants isolated +
  auditable.

### Wave 3 — Privacy, optional integrations, desktop-equivalent surfaces

#### MOB-030 — Biometric lock, encrypted backups, granular data controls
- Status: **PARTIAL** · Type: AFK impl (HITL biometric/restore/erase) · Blocked by: MOB-002, MOB-003
- Owns: `persistence/backup/EncryptedBackupCodec.kt`, `feature/MoreScreen.kt` (backup),
  `core/vault/AndroidKeystoreVaultCrypto.kt`.
- Current: encrypted backup codec + restore validation exist.
- **Gap:** biometric/PIN gate, passphrase-encrypted versioned backup with checksum, selective
  preview/restore, per-module export/delete, Keystore-invalidation recovery.
- Acceptance: backup unreadable without passphrase + tamper-rejected; restore validates fully
  before replacing live data; tokens/model files excluded unless selected; lock timeout +
  recovery tested on S25.

#### MOB-031 — Settings, themes, local diagnostics
- Status: **PARTIAL** · Type: AFK · Blocked by: MOB-002, MOB-004, MOB-012, MOB-022
- Owns: `feature/DiagnosticsPanel.kt`, `core/diagnostics/DiagnosticsDomain.kt`,
  `core/diagnostics/RedactedDiagnosticsExporter.kt`, `feature/MoreScreen.kt` (settings).
- Current: diagnostics + redacted export + cleanup exist.
- **Gap:** theme/font/density/accessibility, active model, privacy, voice, storage controls;
  feature-availability distinguishes missing permission/model/network.
- Acceptance: settings persist + apply without corrupting active work; diagnostics export
  redacts private text; storage usage/cleanup targets accurate.

#### MOB-032 — Secure optional model-provider vault
- Status: **PARTIAL** · Type: AFK adapter (HITL account/key/OAuth) · Blocked by: MOB-012, MOB-030
- Owns: `core/vault/VaultDomain.kt`, `core/vault/AndroidKeystoreVaultCrypto.kt`,
  `feature/ProviderPanel.kt`.
- Current: Keystore vault + provider panel exist.
- **Gap:** provider adapters (API tokens, device flow, ChatGPT/Codex/Copilot), add/remove/
  test provider, streaming + clear rate-limit/auth errors, hard local-only network gate.
- Acceptance: core never requires a provider; local-only blocks all provider traffic
  centrally; secrets never render/log/enter backups; account removal deletes credentials +
  cached remote metadata.

#### MOB-033 — Web search + deep research
- Status: **MISSING** · Type: AFK workflow (HITL provider/network consent) · Blocked by: MOB-022, MOB-024, MOB-032
- Owns: new `feature/SearchScreen.kt` + `core/research/` package + archive.
- Build: explicitly consented search, progress/cancel, safe URL opening, cited report
  generation, local source/result archive.
- Acceptance: every claim links to stored source URL/title/time; network use visible + cancel
  stops pending work; reports readable offline; local-only disables without breaking app.

#### MOB-034 — Email integration
- Status: **MISSING** · Type: AFK compose bridge (HITL mailbox/OAuth) · Blocked by: MOB-003, MOB-030
- Owns: new `feature/EmailScreen.kt` + `core/email/` adapter.
- Build: reviewed ACTION_SENDTO compose first; optional OAuth/IMAP inbox/search/read/
  attachments with encrypted tokens + notification policy.
- Acceptance: compose shows recipient/subject/body/attachments before leaving app; core works
  with no mail account; account removal clears credentials + cached mail; no automatic send
  from model output.

#### MOB-035 — Desktop-only capability replacement guide
- Status: **MISSING** · Type: AFK (HITL final equivalence sign-off) · Blocked by: MOB-003, MOB-024, MOB-025
- Owns: `docs/desktop-capability-inventory.json` + in-app help.
- Build: map desktop shell/Docker/workspace/MCP/browser-automation to sandboxed phone
  capabilities; clearly identify physically unavailable operations.
- Acceptance: every desktop route mapped to native / optional-network / explicitly impossible;
  no hidden PC/Pi/server dependency; help explains Android replacements in plain language;
  parity matrix has no unclassified route.

### Wave 4 — Ship gate

#### MOB-040 — Automated parity + regression suite
- Status: **PARTIAL** · Type: AFK · Blocked by: all required feature tickets
- Owns: unit/repository/contract/Compose/screenshot tests.
- Current: unit + contract tests exist.
- **Gap:** Compose flows, screenshot tests, S25 config tests, release lint clean.
- Acceptance: clean suite from one documented command; every parity row has ≥1 verification
  reference; permission-denied/offline/corrupt-file/low-storage covered; no blocking lint.

#### MOB-041 — Physical Galaxy S25 verification + soak
- Status: **NOT STARTED** · Type: HITL · Blocked by: MOB-040
- Owns: device test evidence + performance envelope.
- Build: install/upgrade, portrait/landscape, IME/back, permissions grant/revoke, airplane
  mode, process death, reboot reminders, storage pressure, accessibility, 30-min E2B/E4B.
- Acceptance: no critical crash/ANR/data-loss/clipped UI; battery/memory/thermal/first-token/
  throughput recorded; upgrade preserves data; all consequential actions confirmed.

#### MOB-042 — Signed release APK + final handoff
- Status: **NOT STARTED** · Type: AFK packaging (HITL signing key/install) · Blocked by: MOB-041
- Owns: release config + artifact.
- Build: final version, stable signing key, R8/resources, privacy/help text, reproducible
  artifact, checksum, install guide.
- Acceptance: signature + SHA-256 verify; clean install + signed upgrade pass on S25; launches
  + core local flow in airplane mode; final limitations contain no unclassified/missing feature.

---

## 5. Parallel execution map

```
Wave 0 (gate):  MOB-001 [DONE] → MOB-002 [DONE]
                MOB-004 (parallel with MOB-002) → MOB-003
Wave 1 (core):  MOB-010, MOB-011, MOB-013, MOB-014, MOB-015, MOB-016, MOB-017, MOB-018
                (respect blockers; max 3 feature owners at once)
Wave 2 (intel): MOB-012, MOB-020, MOB-021 → MOB-022 → MOB-023, MOB-024, MOB-025
Wave 3 (trust): MOB-030, MOB-031 → MOB-032, MOB-033, MOB-034, MOB-035
Wave 4 (ship):  MOB-040 → MOB-041 → MOB-042
```

**Concurrency:** after Wave 0, max **4 agents** (1 integration owner + 3 feature owners).
The integration owner merges in dependency order and owns build-file/navigation/schema
conflicts. Feature owners never edit `app/build.gradle.kts`, `settings.gradle.kts`,
`navigation/Destination.kt`, or Room schema files unless their ticket owns them.

## 6. Claim → integrate → handoff protocol

1. **Claim:** pick an unclaimed ticket; announce ownership; branch `mob/<ticket>` from the
   green baseline.
2. **Implement:** thin end-to-end slice; add tests; keep changes inside the declared module.
3. **Verify:** green compile + tests on the PC; contract checks on the Pi; no new lint
   blockers; no private content in logs.
4. **Integrate:** rebase onto the latest baseline; the integration owner merges in dependency
   order; shared-contract changes get foundation-owner review first.
5. **Handoff:** mark the ticket's acceptance boxes; update `SWARM_BACKLOG.md` status; leave a
   one-paragraph summary (what changed, files, tests, any HITL follow-up).

## 7. Definition of shipped

All standalone-core tickets through MOB-042 pass; release APK signed + upgrade-tested; S25
verification matrix has no critical failures; network integrations optional; local-only mode
works in airplane mode; desktop shell/Docker/host-filesystem/browser-automation are
permissioned Android capabilities, never hidden PC/Pi dependencies.
