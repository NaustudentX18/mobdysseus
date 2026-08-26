# Mobdysseus Full-Parity Swarm Backlog & Master Work Orders

Status legend: `AFK` can be completed unattended; `HITL` requires human-in-the-loop (e.g. physical device test, account consent).

## Definition of Shipped (v2.0.0 Completed)
Mobdysseus v2.0.0 is shipped: all standalone-core tickets through MOB-042 pass, the release APK is signed and upgrade-tested, all 59/59 desktop routes are classified and covered, 7-theme dynamic engine is integrated, and all automated contract suites pass.

---

## 🏛️ Phase 1: Foundation & Modular Core (v1.0.0 — SHIPPED)

### MOB-001 — Modularize app shell into feature packages
- Status: **DONE** (v1.0.0) · Type: AFK · Blocked by: None
- Owns: `MainActivity.kt`, `navigation/Destination.kt`, dependency wiring.
- Acceptance: Boundary checks pass across 67 Kotlin files.

### MOB-002 — Typed encrypted offline data foundation
- Status: **DONE** (v1.0.0) · Type: AFK · Blocked by: MOB-001
- Owns: `persistence/**`, Room Database Schema v4 + SQLCipher, Android Keystore encryption.
- Acceptance: Room migrations and encrypted CRUD survive force-stop and reboots.

### MOB-003 — Android capability broker and approval ledger
- Status: **DONE** (v1.0.0) · Type: AFK / HITL · Blocked by: MOB-001, MOB-002
- Owns: `capability/CapabilityPolicy.kt`, `capability/CapabilityAuditPersistence.kt`.
- Acceptance: Typed allowlist, webhook delivery, mutating actions require physical tap.

### MOB-004 — S25 adaptive shell and accessibility baseline
- Status: **DONE** (v1.0.0) · Type: AFK · Blocked by: MOB-001
- Owns: Insets, IME padding, adaptive navigation rail vs bottom bar.
- Acceptance: Zero UI clipping with IME active, 48dp touch targets, TalkBack semantics.

### MOB-006 — Transactional v0 to v1 workspace database migration codec
- Status: **DONE** (v1.0.0) · Type: AFK · Blocked by: MOB-002
- Owns: `persistence/V0WorkspaceMigrationCodec.kt`.
- Acceptance: Validates legacy JSON payloads and migrates cleanly into Room tables.

### MOB-007 — Zero-network airplane-mode runtime enforcement
- Status: **DONE** (v1.0.0) · Type: AFK · Blocked by: MOB-001
- Owns: Offline-first architecture guarantees.
- Acceptance: App operates with 100% functionality under Android Airplane Mode.

---

## 🎨 Phase 2: Parity, Theming & Sampler Tuning (v2.0.0 — SHIPPED)

### MOB-005 — Dynamic 7-theme OLED color engine & Material You
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-001, MOB-002
- Owns: `core/theme/ThemeDomain.kt`, `ui/MobdysseusTheme.kt`, `MoreScreen` theme switcher.
- Acceptance: 7 custom OLED palettes with live interactive previews and Room persistence.

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

### MOB-030 — Biometric lock, encrypted backups and granular wipe
- Status: **DONE** (v2.0.0) · Type: AFK / HITL · Blocked by: MOB-002, MOB-003
- Owns: `persistence/backup/EncryptedBackupCodec.kt`, wipe controls.
- Acceptance: AES-256-GCM encrypted `.mobdbak` exports, granular wipe per module.

### MOB-031 — Settings, themes and local diagnostics
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-002, MOB-004
- Owns: `core/diagnostics/RedactedDiagnosticsExporter.kt`, Theme Engine, Sampler Knobs.
- Acceptance: Redacted diagnostics export, storage usage breakdown, full settings controls.

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

### MOB-036 — Room Database Schema v5 & SQLCipher Migrations
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-002, MOB-031
- Owns: `MobdysseusDatabase.kt` (`MIGRATION_3_4`, `MIGRATION_4_5`).
- Acceptance: Automated transactional migration adding 14 settings columns without data loss.

### MOB-037 — LiteRT GPU-to-CPU Fallback Recovery
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: MOB-012
- Owns: `LocalModelRuntime.kt` load method.
- Acceptance: Graceful recovery to CPU backend when GPU delegates are unavailable.

### MOB-038 — Brand Identity & Vector Graphic Assets
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: None
- Owns: `assets/logo.svg`, `assets/banner.png`, `app/src/main/res/drawable/ic_odysseus.xml`.
- Acceptance: High-resolution scalable vector branding and adaptive icons.

### MOB-040 — Automated parity and regression suite
- Status: **DONE** (v2.0.0) · Type: AFK · Blocked by: All feature tickets
- Owns: Boundary validator, contract tests (`pytest tools/`).
- Acceptance: 14/14 contract tests green, 67/67 Kotlin files compliant.

### MOB-041 — Physical Galaxy S25 verification and soak
- Status: **DONE** (v2.0.0) · Type: HITL · Blocked by: MOB-040
- Owns: Device test evidence and performance envelope.
- Acceptance: Zero ANRs, thermal status safe, battery drain `<8%/hr` during inference.

### MOB-042 — Signed release APK and final handoff
- Status: **DONE** (v2.0.0) · Type: AFK Packaging / HITL Key · Blocked by: MOB-041
- Owns: Release build configuration, R8 rules, SHA-256 checksums.
- Acceptance: Signed release APK produced and verified.

---

## ⚡ Phase 3: Snapdragon 8 Elite NPU & High-Throughput Inference (v3.0.0)

### MOB-050 — Qualcomm AI Engine Direct (QNN) HTP Backend
- Status: **READY** · Type: AFK / HITL · Blocked by: MOB-012
- Owns: `LocalModelRuntime.kt`, LiteRT QNN delegate compilation.
- Scope: Wire Qualcomm AI Engine Direct (QNN) delegate via LiteRT CompiledModel API for Snapdragon 8 Elite Hexagon NPU.
- Acceptance:
  - [ ] S25 NPU hardware acceleration is confirmed active in diagnostics.
  - [ ] Sub-150ms Time-to-First-Token (TTFT) achieved.

### MOB-051 — INT4/INT8 Quantization & Qualcomm AI Hub Integration
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-050
- Owns: Model packaging, SHA-256 envelope verifier.
- Scope: Support INT4/INT8 mixed-precision weights (Gemma 2 2B, Llama 3.2 3B) pre-optimized for Snapdragon NPU.
- Acceptance:
  - [ ] Memory footprint reduced to `<1.8 GB RAM` during active inference.
  - [ ] Exact SHA-256 checksum verification before loading.

### MOB-052 — Throughput & Latency Optimization
- Status: **PLANNED** · Type: AFK / HITL · Blocked by: MOB-050
- Owns: Streaming token assembler, tensor buffer management.
- Scope: Achieve sustained >45 tokens/second streaming generation on Galaxy S25 NPU.
- Acceptance:
  - [ ] Benchmark verifies >45 tok/sec across 1,000 generated tokens.

### MOB-053 — Dynamic Thermal-Governed Context Window Scaling
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-050
- Owns: `LocalModelRuntime.kt`, Android ThermalManager listener.
- Scope: Dynamically scale context window and batch size based on device thermal status.
- Acceptance:
  - [ ] Zero thermal throttling shutdowns during 30-minute stress tests.

### MOB-054 — Low-Power Battery Profiler
- Status: **PLANNED** · Type: HITL · Blocked by: MOB-052
- Scope: Optimize CPU/NPU power state transitions.
- Acceptance:
  - [ ] Battery drain measured at `<5% per hour` during continuous generation.

### MOB-055 — Multi-Model Hot-Swapping
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-050
- Scope: Fast memory unmapping and loading between reasoning models and code models.
- Acceptance:
  - [ ] Model switch latency `<800ms` on Snapdragon 8 Elite.

---

## 👁️ Phase 4: Edge Multimodal Vision & Audio Intelligence (v3.5.0)

### MOB-060 — On-Device Vision Language Model Runtime
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-050
- Scope: Embed lightweight on-device vision encoder (SmolVLM / PaliGemma 2 / Moondream 2).
- Acceptance:
  - [ ] Unified vision-language prompt assembler for image + text queries.

### MOB-061 — Real-Time Visual Document & Diagram Q&A
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-021, MOB-060
- Scope: Extract tables, charts, and diagrams directly from imported PDF/PNG files.
- Acceptance:
  - [ ] Zero image data leaves the device; 100% on-device vision inference.

### MOB-062 — Private Gallery Visual Inspector & Scrubbing
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-017, MOB-060
- Scope: Multimodal image tagging, captioning, and non-destructive visual metadata stripping.
- Acceptance:
  - [ ] Automatic semantic tag generation for gallery photos.

### MOB-063 — On-Device Streaming Speech-to-Text (STT)
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-016
- Scope: Embed on-device Whisper.tflite / Sherpa-ONNX streaming transcriber.
- Acceptance:
  - [ ] Real-time voice dictation with automatic punctuation and language detection.

### MOB-064 — Neural Text-to-Speech (TTS) Engine
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-016
- Scope: High-quality on-device neural voice synthesizer (Piper TTS / Sherpa-ONNX VITS).
- Acceptance:
  - [ ] Natural voice playback with background audio focus ducking.

---

## 🧠 Phase 5: Dense Vector RAG & Semantic Memory Matrix (v4.0.0)

### MOB-070 — On-Device Dense Embedding Runtime
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-050
- Scope: Embed `bge-micro-v2` / `nomic-embed-text-v1.5` 384-dimensional sentence transformer.
- Acceptance:
  - [ ] >100 chunks/sec embedding throughput on Hexagon NPU.

### MOB-071 — Native Vector Storage in Room / SQLCipher
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-002, MOB-070
- Scope: Embed `sqlite-vec` or native HNSW vector index inside the encrypted SQLCipher container.
- Acceptance:
  - [ ] Sub-10ms cosine similarity lookups across 10,000+ chunks.

### MOB-072 — Hybrid Reciprocal Rank Fusion (RRF)
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-022, MOB-071
- Scope: Combine BM25 lexical keyword matching with dense semantic embeddings.
- Acceptance:
  - [ ] Higher retrieval accuracy and resilience against vocabulary mismatch.

### MOB-073 — Hierarchical Provenance Chunking & Citation Badges
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-021, MOB-072
- Scope: Structure-aware chunking with interactive UI citation badges linking to highlighted source PDFs.
- Acceptance:
  - [ ] Tapping a citation pill opens the document viewer scrolled to the exact paragraph.

### MOB-074 — Mnemosyne Graph Memory Engine
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-023, MOB-070
- Scope: Autonomous entity-relationship graph extraction and temporal memory decay.
- Acceptance:
  - [ ] Graph visualization of user concepts and automatic recall of relevant facts.

### MOB-075 — Memory Quarantine & Prompt-Injection Filter
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-023
- Scope: Pre-execution classifier inspecting ingested memories for adversarial injections.
- Acceptance:
  - [ ] Quarantine review gate catches 100% of injected prompt overrides.

---

## 🤖 Phase 6: Multi-Agent Swarms & Sandboxed Tool Mesh (v4.5.0)

### MOB-080 — Hierarchical Multi-Agent Coordinator
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-024, MOB-050
- Scope: On-device agent loop orchestrating specialized subagents (Planner, Researcher, FactChecker, Coder).
- Acceptance:
  - [ ] Subagents communicate over typed immutable message channels with synthesized output.

### MOB-081 — Grammar-Constrained JSON Schema Enforcement
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-012
- Scope: BNF / GBNF grammar constraints forcing LLM output into strict JSON schemas.
- Acceptance:
  - [ ] Zero JSON parse errors during tool calling.

### MOB-082 — Cryptographically Signed Skill Packs
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-025
- Scope: ECDSA signature verification for declarative skill packs.
- Acceptance:
  - [ ] Unsigned or tampered skill packs are rejected automatically.

### MOB-083 — Interactive Physical Approval Sheets
- Status: **PLANNED** · Type: HITL · Blocked by: MOB-003, MOB-080
- Scope: Bottom sheet modal requiring physical user tap for mutating phone actions.
- Acceptance:
  - [ ] Diff preview shown prior to execution.

### MOB-084 — Immutable Capability Audit Ledger
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-003
- Scope: Cryptographic hash-chained audit log recording every tool invocation.
- Acceptance:
  - [ ] Exportable JSON audit trail for security reviews.

### MOB-085 — Consented Deep Web Research Archiver
- Status: **PLANNED** · Type: AFK / HITL · Blocked by: MOB-033
- Scope: Privacy-preserving web search with readability parser and cited markdown reports.
- Acceptance:
  - [ ] Full citation provenance for every research claim.

---

## 🌐 Phase 7: Decentralized Mesh & Multi-Device Ecosystem (v5.0.0+)

### MOB-090 — Wi-Fi Aware & Tailscale P2P Agent Mesh
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-003, MOB-032
- Scope: Zero-configuration local device discovery using Android Wi-Fi Aware (NAN) and Tailscale mTLS.
- Acceptance:
  - [ ] Secure rendezvous between Galaxy S25, M5Stack Cardputer ADV, and Pi AiServer.

### MOB-091 — Heavy Compute Offload (OMP/Hermes Protocol)
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-090
- Scope: Offload long-running batch runs or model training to home PC over Tailscale.
- Acceptance:
  - [ ] Automatic fallback to local NPU when off-grid.

### MOB-092 — Samsung DeX Desktop Multi-Window Mode
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-004
- Scope: Desktop UI layout for Samsung DeX with multi-window support and drag-and-drop.
- Acceptance:
  - [ ] Seamless drag-and-drop document import and keyboard navigation.

### MOB-093 — Wear OS / Galaxy Watch Companion App
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-016
- Scope: Companion Wear OS app for quick voice dictation and agent status telemetry.
- Acceptance:
  - [ ] Standalone watch app connects to phone over Bluetooth Low Energy (BLE).

### MOB-094 — Peer-to-Peer Encrypted Workspace Sync
- Status: **PLANNED** · Type: AFK · Blocked by: MOB-090
- Scope: Conflict-Free Replicated Data Types (CRDTs) for multi-device sync without central servers.
- Acceptance:
  - [ ] Zero-loss peer synchronization across mobile, tablet, and PC.
