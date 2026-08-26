# Mobdysseus Full-Parity Swarm Backlog & Work Orders

Status legend: `AFK` can be completed unattended; `HITL` requires human-in-the-loop (e.g. physical device test, account consent).

## Definition of Shipped (v2.0.0 Completed)
Mobdysseus v2.0.0 is shipped: all standalone-core tickets through MOB-042 pass, the release APK is signed and upgrade-tested, all 59/59 desktop routes are classified and covered, 7-theme dynamic engine is integrated, and all automated contract suites pass.

## Wave 0 — Foundation & Modular Core

### MOB-001 — Modularize app shell into feature packages
- Status: **DONE** (v1.0.0) · Type: AFK · Blocked by: None
- Owns: `MainActivity.kt`, `navigation/Destination.kt`, dependency wiring.
- Acceptance: Boundary checks pass across 67 Kotlin files.

### MOB-002 — Typed encrypted offline data foundation
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-001
- Owns: `persistence/**`, Room Database Schema v4 + SQLCipher, Android Keystore encryption.
- Acceptance: Room migrations and encrypted CRUD survive force-stop and reboots.

### MOB-003 — Android capability broker and approval ledger
- Status: **DONE** (v2.0.0) · Type: AFK / HITL · Blocked by: MOB-001, MOB-002
- Owns: `capability/CapabilityPolicy.kt`, `capability/CapabilityAuditPersistence.kt`.
- Acceptance: Typed allowlist, webhook delivery, mutating actions require physical tap.

### MOB-004 — S25 adaptive shell and accessibility baseline
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-001
- Owns: Insets, IME padding, adaptive navigation rail vs bottom bar.
- Acceptance: Zero UI clipping with IME active, 48dp touch targets, TalkBack semantics.

### MOB-005 — Dynamic 7-theme OLED color engine & Material You
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-001, MOB-002
- Owns: `core/theme/ThemeDomain.kt`, `ui/MobdysseusTheme.kt`, `MoreScreen` theme switcher.
- Acceptance: 7 custom OLED palettes with live interactive previews and Room persistence.

## Wave 1 — Core Product Slices

### MOB-010 — Full conversation and streaming message workflow
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-002, MOB-004
- Owns: `feature/ChatScreen.kt`, conversation repository.
- Acceptance: Offline local chat streaming, Markdown/code rendering, JSON/text export.

### MOB-011 — Verified local model registry and storage manager
- Status: **DONE** (v2.0.0) · Type: AFK / HITL · Blocked by: MOB-002
- Owns: `LocalModelRuntime.kt`, model catalog, SHA-256 verification.
- Acceptance: SAF import, device-fit profiler, app-private model storage.

### MOB-012 — Streaming and cancellable LiteRT-LM runtime
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-011
- Owns: Foreground inference service, token streaming, memory management.
- Acceptance: Zero UI lockup, dynamic context budgeting, foreground service keep-alive.

### MOB-013 — Private files, attachments and Android share intake
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-002, MOB-003
- Owns: `core/file/`, `core/document/`, ACTION_SEND receiver.
- Acceptance: Scoped storage import with SHA-256 deduplication and quarantine.

### MOB-014 — Notes parity and tag taxonomy
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-002, MOB-004, MOB-013
- Owns: `feature/WorkspaceFeatureScreens.kt` Notes section.
- Acceptance: Markdown notes, tag taxonomy, instant search, encrypted storage.

### MOB-015 — Tasks, recurrence and local reminders
- Status: **DONE** (v2.0.0) · Type: AFK / HITL · Blocked by: MOB-002, MOB-003
- Owns: `core/task/`, WorkManager exact alarm scheduler.
- Acceptance: Due dates, recurrence engine, notifications with deep links.

### MOB-016 — Complete offline voice workflow
- Status: **DONE** (v2.0.0) · Type: AFK / HITL · Blocked by: MOB-003, MOB-010
- Owns: `core/voice/`, `SafeTextSpeaker.kt`.
- Acceptance: Offline dictation detection, draft review, TTS with audio focus.

### MOB-017 — Private gallery and touch image editor
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-003, MOB-004, MOB-013
- Owns: `core/image/`, `feature/MoreScreen.kt` Gallery section.
- Acceptance: Camera/picker, non-destructive rotation, metadata scrubbing, export.

### MOB-018 — Calendar and contacts parity
- Status: **DONE** (v2.0.0) · Type: HITL · Blocked by: MOB-002, MOB-003
- Owns: `AndroidCalendar.kt`, `AndroidContacts.kt`.
- Acceptance: Scoped Calendar and Contacts Providers behind runtime consent.

## Wave 2 — Intelligence & RAG

### MOB-020 — Executable Cookbook and run history
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-003, MOB-012
- Owns: `core/recipe/`, `feature/CookbookScreen.kt`.
- Acceptance: Versioned recipes, parameter controls, benchmark diagnostics.

### MOB-021 — Offline document extraction library
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-013
- Owns: `core/document/DocumentIngestion.kt`.
- Acceptance: Offline text/markdown/PDF extraction with page provenance.

### MOB-022 — Citation-backed local RAG
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-012, MOB-021
- Owns: `core/retrieval/LocalLexicalIndex.kt`.
- Acceptance: Lexical BM25 search, chunk offset tracking, chat citation badges.

### MOB-023 — Governed semantic Brain memory
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-002, MOB-012
- Owns: `core/memory/MemoryGovernance.kt`.
- Acceptance: Mnemosyne memory matrix, review queue preventing prompt injection.

### MOB-024 — Safe agent executor
- Status: **DONE** (v2.0.0) · Type: AFK Engine / HITL Approvals · Blocked by: MOB-003, MOB-020
- Owns: Agent planning and execution loop with approval bottom sheets.
- Acceptance: Sequential execution, rollback ledger, zero unapproved side effects.

### MOB-025 — Declarative skills library
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-003, MOB-024
- Owns: `core/skills/SkillPackVerifier.kt`.
- Acceptance: Cryptographically signed skill packs, capability scope inspection.

## Wave 3 — Trust, Vault & Mobile Ecosystem

### MOB-030 — Biometric lock, encrypted backups and granular wipe
- Status: **DONE** (v2.0.0) · Type: AFK / HITL · Blocked by: MOB-002, MOB-003
- Owns: `persistence/backup/EncryptedBackupCodec.kt`, wipe controls.
- Acceptance: AES-256-GCM encrypted `.mobdbak` exports, granular wipe per module.

### MOB-031 — Settings, themes and local diagnostics
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-002, MOB-004
- Owns: `core/diagnostics/RedactedDiagnosticsExporter.kt`, Theme Engine.
- Acceptance: Redacted diagnostics export, storage usage breakdown, theme toggle.

### MOB-032 — Secure optional model-provider vault
- Status: **DONE** (v2.0.0) · Type: AFK / HITL · Blocked by: MOB-012, MOB-030
- Owns: `core/vault/AndroidKeystoreVaultCrypto.kt`, ProviderPanel.
- Acceptance: Keystore-encrypted API keys with hardware local-only killswitch.

### MOB-033 — Consented web search and deep research
- Status: **DONE** (v2.0.0) · Type: AFK / HITL · Blocked by: MOB-022, MOB-024, MOB-032
- Owns: Consented DuckDuckGo client and offline report citation archiver.
- Acceptance: Explicit consent, offline report caching, zero background tracking.

### MOB-034 — Email compose and optional sync
- Status: **DONE** (v2.0.0) · Type: AFK / HITL · Blocked by: MOB-003, MOB-030
- Owns: Reviewed `ACTION_SENDTO` compose bridge and optional IMAP adapter.
- Acceptance: Human confirmation before sending, encrypted account credentials.

### MOB-035 — Desktop-only capability replacement guide
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: All
- Owns: `docs/desktop-capability-inventory.json`, help guides.
- Acceptance: 59/59 desktop routes mapped to native or sandboxed mobile equivalents.

## Wave 4 — Ship Gate & Release

### MOB-040 — Automated parity and regression suite
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: All feature tickets
- Owns: Boundary validator, contract tests (`pytest tools/`).
- Acceptance: 10/10 contract tests green, 67/67 Kotlin files compliant.

### MOB-041 — Physical Galaxy S25 verification and soak
- Status: **DONE** (v2.0.0) · Type: HITL · Blocked by: MOB-040
- Owns: Device test evidence and performance envelope.
- Acceptance: Zero ANRs, thermal status safe, battery drain `<8%/hr` during inference.

### MOB-042 — Signed release APK and final handoff
- Status: **DONE** (v2.0.0) · Type: AFK Packaging / HITL Key · Blocked by: MOB-041
- Owns: Release build configuration, R8 rules, SHA-256 checksums.
- Acceptance: Signed release APK produced and verified.

---

## 🔮 Next Horizon: Wave 5 — Edge Multimodal & Snapdragon NPU (v3.0.0)

### MOB-050 — LiteRT CompiledModel API & Snapdragon QNN Backend
- Type: AFK (Build) / HITL (Benchmark) · Blocked by: MOB-012
- Scope: Wire Qualcomm AI Engine Direct (QNN) delegates via LiteRT CompiledModel API.

### MOB-051 — On-Device Vision Parser & Visual RAG
- Type: AFK · Blocked by: MOB-013, MOB-021
- Scope: Embed lightweight on-device vision encoder (SmolVLM / PaliGemma 2).

### MOB-052 — SQLite-Vec Dense Vector Search & Hybrid RRF
- Type: AFK · Blocked by: MOB-021, MOB-022
- Scope: On-device embedding generation (BGE-Micro) stored in SQLite vector tables.

---

## 🌐 Next Horizon: Wave 6 — Decentralized Mesh & Mobile Swarms (v4.0.0)

### MOB-060 — Wi-Fi Aware & Tailscale P2P Agent Mesh
- Type: AFK · Blocked by: MOB-003, MOB-032
- Scope: Private local agent discovery and task handoff between Galaxy S25 and Pi AiServer.

### MOB-061 — Multi-Agent Subagent Coordinator
- Type: AFK · Blocked by: MOB-024, MOB-060
- Scope: Spawn and orchestrate parallel specialized on-device subagents.
