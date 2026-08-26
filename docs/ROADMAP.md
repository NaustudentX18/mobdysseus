# 🗺️ Mobdysseus Master Engineering Roadmap

The comprehensive, battle-tested master roadmap for **Mobdysseus** — the sovereign, local-first on-device AI workspace engineered for the Samsung Galaxy S25, Android 15+, and the edge computing mesh.

Legend:
* `[x]` **Completed & Shipped in Repository**
* `[ ]` **Backlog / Next Horizon Target**

---

## 🏛️ Phase 1: Foundation & Modular App Shell (v1.0.0 — SHIPPED)

- [x] **MOB-001** Strict unidirectional package architecture (`check_dependency_boundaries.py` enforcing `shell -> navigation -> feature -> core`).
- [x] **MOB-002** Encrypted local data foundation: Room Database with SQLCipher AES-256-GCM and Android Keystore root of trust.
- [x] **MOB-003** Sandboxed Android capability broker with interactive physical approval sheets and audit ledger.
- [x] **MOB-004** Adaptive Samsung Galaxy S25 shell (edge-to-edge layout, IME safe insets, navigation rail vs bottom bar, 48dp touch targets, TalkBack accessibility).
- [x] **MOB-006** Transactional v0 to v1 workspace database migration codec with referential integrity verification.
- [x] **MOB-007** Zero-network enforcement: guaranteed full functionality under airplane mode.

---

## 🎨 Phase 2: Parity, Theming & Sampler Tuning (v2.0.0 — SHIPPED)

- [x] **MOB-005** Dynamic 7-Theme Color Engine (`AppTheme`, `ThemeDomain.kt`, `MobdysseusTheme.kt` with OLED power optimization and Material You dynamic color extraction).
- [x] **MOB-035** 100% Desktop Route Parity Contract: 59/59 CutiePie Odysseus routes mapped to native mobile equivalents in `docs/desktop-capability-inventory.json`.
- [x] **MOB-031** Comprehensive Settings Suite in `MoreScreen.kt`:
  - [x] Fine-grained inference sampler controls (`temperature`, `topP`, `topK`, `maxTokens`).
  - [x] Editable system instruction prompt.
  - [x] Configurable RAG context depth (`ragTopK`).
  - [x] Voice TTS engine speech rate & pitch controls.
  - [x] Editor & automation controls (autosave drafts, markdown rendering, notifications).
- [x] **MOB-036** Database Schema v5 upgrade (`MIGRATION_3_4`, `MIGRATION_4_5`) with automated SQLCipher migrations.
- [x] **MOB-037** LiteRT runtime resilience: automatic `GPU -> CPU` fallback recovery preventing startup crashes.
- [x] **MOB-038** Visual Identity Overhaul: vector logo (`assets/logo.svg`, `logo.png`), hero banner (`banner.png`), Android vector icon (`ic_odysseus.xml`), and 4 high-res UI screenshots.
- [x] **MOB-040** Automated Contract Test Suite: 14/14 pytest contract and boundary tests passing in CI/host.

---

## ⚡ Phase 3: Snapdragon 8 Elite NPU & High-Throughput Inference (v3.0.0)

- [ ] **MOB-050** **Qualcomm AI Engine Direct (QNN) HTP Backend:**
  - [ ] Integrate LiteRT Qualcomm QNN Delegate (`com.qualcomm.qti:qnn-litert-delegate:2.34.0+`).
  - [ ] Configure `HTP_BACKEND` targeting Snapdragon 8 Elite (SM8750) Hexagon NPU.
  - [ ] Implement LiteRT `CompiledModel` API for pre-compiled ahead-of-time (AOT) model graphs.
- [ ] **MOB-051** **INT4/INT8 Quantization & Qualcomm AI Hub Integration:**
  - [ ] Support INT4/INT8 mixed-precision weights (Gemma 2 2B, Llama 3.2 3B, Qwen 2.5 3B).
  - [ ] Implement SHA-256 integrity check and model envelope validation prior to loading.
- [ ] **MOB-052** **Throughput & Latency Optimization:**
  - [ ] Sub-150ms Time-To-First-Token (TTFT) on Snapdragon 8 Elite.
  - [ ] Sustained >45 tokens/second streaming generation on NPU.
- [ ] **MOB-053** **Thermal Governance & Memory Profiler:**
  - [ ] Dynamic context window scaling based on device thermal status (`PowerManager.OnThermalStatusChangedListener`).
  - [ ] Zero-copy tensor buffer sharing with Android Ashmem / AHardwareBuffer.
- [ ] **MOB-054** **Battery Drain Optimization:**
  - [ ] Energy consumption profiling targeting `<5% battery drain per hour` of active continuous inference.
- [ ] **MOB-055** **Multi-Model Hot-Swapping:**
  - [ ] Low-latency memory unmapping and fast loading between reasoning models and specialized code models.

---

## 👁️ Phase 4: Edge Multimodal Vision & Audio Intelligence (v3.5.0)

- [ ] **MOB-060** **On-Device Vision Language Model Runtime:**
  - [ ] Embed lightweight edge VLM (SmolVLM / PaliGemma 2 / Moondream 2) compiled for LiteRT NPU.
  - [ ] Unified vision-language prompt assembler for image + text queries.
- [ ] **MOB-061** **Real-Time Visual Document & Diagram Q&A:**
  - [ ] Extract tables, flowcharts, and architecture diagrams directly from imported PDF/PNG documents.
  - [ ] Zero-cloud visual inspection: 100% of pixel processing remains in app-private memory.
- [ ] **MOB-062** **Private Gallery Visual Inspector:**
  - [ ] Multimodal image inspection, visual captioning, and semantic object tagging for gallery photos.
  - [ ] Non-destructive visual metadata stripping prior to external exports.
- [ ] **MOB-063** **On-Device Speech Recognition (STT):**
  - [ ] Embed on-device Whisper.tflite / Moonshine / Sherpa-ONNX streaming audio transcriber.
  - [ ] Low-latency real-time voice dictation with automatic punctuation and language detection.
- [ ] **MOB-064** **Neural Text-to-Speech (TTS):**
  - [ ] Embed high-quality on-device neural voice synthesizer (Piper TTS / Sherpa-ONNX VITS).
  - [ ] Natural conversational playback with pause/resume and background audio focus ducking.

---

## 🧠 Phase 5: Dense Vector RAG & Semantic Memory Matrix (v4.0.0)

- [ ] **MOB-070** **On-Device Dense Embedding Runtime:**
  - [ ] Embed lightweight on-device sentence transformer (`bge-micro-v2` / `nomic-embed-text-v1.5` 384-dim).
  - [ ] High-throughput batch embedding generation on Hexagon NPU (>100 chunks/sec).
- [ ] **MOB-071** **Native Vector Storage in Room / SQLCipher:**
  - [ ] Embed `sqlite-vec` C-extension or native HNSW vector index inside the encrypted SQLCipher container.
  - [ ] Perform cosine similarity vector lookups with sub-10ms query latency across 10,000+ chunks.
- [ ] **MOB-072** **Hybrid Reciprocal Rank Fusion (RRF):**
  - [ ] Combine lexical BM25 keyword matching with dense semantic embeddings via RRF scoring.
  - [ ] Dynamic threshold filtering to exclude low-relevance noise.
- [ ] **MOB-073** **Provenance Chunking & Citation Badges:**
  - [ ] Hierarchical semantic chunking preserving document structure, section headers, and page offsets.
  - [ ] Interactive UI citation pills linking directly to the highlighted source excerpt in the Document viewer.
- [ ] **MOB-074** **Mnemosyne Graph Memory Engine:**
  - [ ] Automatic entity and relationship extraction from ongoing conversations.
  - [ ] Temporal memory decay and relevance scoring over time.
- [ ] **MOB-075** **Memory Quarantine & Prompt-Injection Gate:**
  - [ ] Pre-execution classifier inspecting ingested memories and documents for adversarial prompt injections.
  - [ ] User review sheet for quarantined memories before persistence.

---

## 🤖 Phase 6: Multi-Agent Swarms & Sandboxed Tool Mesh (v4.5.0)

- [ ] **MOB-080** **Hierarchical Multi-Agent Coordinator:**
  - [ ] On-device agent loop orchestrating specialized subagents (*Planner*, *Researcher*, *FactChecker*, *Coder*).
  - [ ] Immutable message channels between subagents with structured progress reporting.
- [ ] **MOB-081** **Grammar-Constrained JSON Schema Enforcement:**
  - [ ] BNF / GBNF grammar constraints forcing LLM output into strict typed JSON schemas.
  - [ ] Zero parser errors during autonomous function calling.
- [ ] **MOB-082** **Cryptographically Signed Skill Packs:**
  - [ ] Skill pack validator verifying ECDSA signatures against trusted publisher keys.
  - [ ] Declarative capability manifest declaring required Android permissions.
- [ ] **MOB-083** **Interactive Consent & Approval Sheets:**
  - [ ] Modal bottom sheets requiring physical user tap for mutating actions (calendar insert, contact write, file export).
  - [ ] Diff preview for proposed changes before execution.
- [ ] **MOB-084** **Immutable Capability Audit Ledger:**
  - [ ] Cryptographic hash-chained audit log recording every tool invocation and user decision.
  - [ ] Exportable JSON audit trail for compliance and debugging.
- [ ] **MOB-085** **Consented Deep Web Research:**
  - [ ] Privacy-preserving web search via DuckDuckGo / SearXNG API with user consent.
  - [ ] HTML readability parser with cited markdown report archiver.

---

## 🌐 Phase 7: Decentralized Mesh & Multi-Device Ecosystem (v5.0.0+)

- [ ] **MOB-090** **Wi-Fi Aware (NAN) & Tailscale P2P Agent Mesh:**
  - [ ] Zero-configuration local device discovery using Android Wi-Fi Aware (Neighbor Awareness Networking).
  - [ ] Encrypted mTLS communication between Galaxy S25, M5Stack Cardputer ADV, and Pi AiServer.
- [ ] **MOB-091** **Heavy Compute Offloading (OMP/Hermes Protocol):**
  - [ ] Seamlessly delegate long-running batch jobs, massive RAG indexing, or model fine-tuning to home PC over Tailscale.
  - [ ] Automatic fallback to local NPU when disconnected from the mesh.
- [ ] **MOB-092** **Samsung DeX Ultra-Workspace Mode:**
  - [ ] Multi-window desktop UI layout optimized for Samsung DeX on external monitors.
  - [ ] Drag-and-drop file ingestion, split-screen document analysis, and full keyboard shortcut mapping.
- [ ] **MOB-093** **Wear OS / Galaxy Watch Companion App:**
  - [ ] Lightweight Wear OS app for quick voice notes, task completion, and agent status notifications.
  - [ ] Biometric sensor context intake (heart rate, step count) behind explicit user permissions.
- [ ] **MOB-094** **Peer-to-Peer Encrypted Workspace Sync:**
  - [ ] Conflict-Free Replicated Data Types (CRDTs) for multi-device sync without central servers.
  - [ ] End-to-end encrypted peer synchronization over local Wi-Fi.

---

## 📊 Summary Parity & Phase Progress

| Phase | Milestone | Focus Area | Status |
|---|---|---|---|
| **Phase 1** | `v1.0.0` | Modular Foundation, SQLCipher Room, S25 Adaptive UI | **100% COMPLETE** |
| **Phase 2** | `v2.0.0` | 7-Theme Engine, Sampler Tuning, 59-Route Parity | **100% COMPLETE** |
| **Phase 3** | `v3.0.0` | Snapdragon 8 Elite NPU, QNN Delegate, LiteRT AOT | **NEXT HORIZON** |
| **Phase 4** | `v3.5.0` | On-Device Multimodal Vision, Whisper STT, Piper TTS | **PLANNED** |
| **Phase 5** | `v4.0.0` | Dense Vector Embeddings, Hybrid RAG, Mnemosyne Matrix | **PLANNED** |
| **Phase 6** | `v4.5.0` | Multi-Agent Swarms, GBNF Grammar, Signed Skill Packs | **PLANNED** |
| **Phase 7** | `v5.0.0+`| Wi-Fi Aware P2P Mesh, Samsung DeX, Wear OS Companion | **PLANNED** |
