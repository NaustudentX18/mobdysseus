# Mobdysseus Strategic Horizon & Roadmap

> **Status:** v2.0.0 Shipped (August 2026).  
> **Target:** Autonomous Edge AI Workspace on Samsung Galaxy S25 / Android 15+.

---

## 🗺️ Master Release Matrix

```
v1.0.0 (Foundation & Native Shell) ──► SHIPPED
  │
v2.0.0 (Theme Engine, SQLCipher, Parity v2) ──► SHIPPED
  │
v3.0.0 (Edge Multimodal & Snapdragon NPU Acceleration) ──► IN PROGRESS
  │
v4.0.0 (Decentralized Edge Mesh & Autonomous Swarms) ──► PLANNED
```

---

## 📦 Phase 1 · v1.0.0 Baseline (Shipped 2026-08-18)
* [x] **Native Kotlin / Compose Shell:** Complete elimination of WebViews and browser wrappers.
* [x] **59-Route Desktop Inventory:** Formal parity mapping for all desktop CutiePie Odysseus modules.
* [x] **Boundary Enforcer:** Automated validation across all Kotlin source files preventing cross-feature spaghetti dependencies.
* [x] **Basic Offline RAG:** Lexical indexing (BM25) over imported private text files.
* [x] **Signed APK Release Pipeline:** Authoritative Windows PC build dispatch via OMP/Hermes bridge.

---

## ⚡ Phase 2 · v2.0.0 Sovereign Workspace (Shipped 2026-08-27)
* [x] **Dynamic Theme Engine:** 7 custom high-contrast OLED color themes (*Obsidian Coral*, *Cyberpunk Neon*, *Midnight Navy*, *Solarized Amber*, *Forest Matrix*, *Monokai Vapor*, and *Material You*).
* [x] **Room Schema v4 + SQLCipher:** AES-256-GCM hardware-backed database encryption via Android Keystore.
* [x] **Mnemosyne Governed Memory:** Long-term semantic memory extraction with explicit user-approval gates to prevent prompt-injection attacks.
* [x] **WorkManager Recurrence Engine:** Recurring cron-like task scheduling with exact alarm notifications surviving reboot.
* [x] **Touch Gallery & Image Editor:** Scoped Photo Picker, non-destructive rotation, metadata scrubbing, and private exports.
* [x] **Redacted Telemetry Exporter:** Hardware and runtime health diagnostics with sensitive prompt text automatically redacted.

---

## 🔮 Phase 3 · v3.0.0 Edge Multimodal & NPU Acceleration (Horizon Q4 2026)

### 3.1 Snapdragon 8 Elite Hexagon NPU Native Pipeline
* **CompiledModel API Integration:** Transition from CPU/GPU delegate negotiation to Qualcomm AI Engine Direct (QNN) via LiteRT CompiledModel API.
* **Quantization Optimization:** First-class support for INT4 and 2-bit weight quantization (AWQ/GPTQ) running sub-300ms time-to-first-token.
* **Dynamic Core Pinning:** Real-time thread affinity pinning prefill phases to Prime/Performance cores and token decode to Efficiency cores.

### 3.2 On-Device Multimodal Vision & Audio
* **Private Image Understanding:** On-device vision encoder (e.g. PaliGemma 2 / SmolVLM) for local screenshot parsing, OCR table extraction, and visual document RAG.
* **Real-Time Voice Streaming:** Bundled Whisper.cpp / Moonshine STT combined with Piper neural TTS for sub-500ms conversational voice interaction without internet access.

### 3.3 Hybrid Dense/Sparse Local RAG
* **On-Device Vector Embeddings:** Integrated BGE-Micro / MiniLM embedding models executed via LiteRT.
* **Hierarchical Vector Index:** SQLite-vec on-device vector indexing coupled with BM25 hybrid reciprocal rank fusion (RRF).

---

## 🌐 Phase 4 · v4.0.0 Decentralized Edge Mesh & Autonomous Swarms (Horizon 2027)

### 4.1 Peer-to-Peer Wi-Fi Aware & Tailscale Swarms
* **Zero-Cloud Agent Mesh:** Nearby Android devices (Cardputer ADV, Raspberry Pi 5 AiServer, Galaxy S25) discover each other via Wi-Fi Aware / Bluetooth LE and coordinate task delegation.
* **Encrypted RPC Wire Protocol:** Mutual TLS (mTLS) over Tailscale for seamless home-server coordination without exposing ports to the public internet.

### 4.2 Multi-Agent Mobile Orchestrator
* **Subagent Spawning:** Mobdysseus orchestrates lightweight specialized subagents on-device (e.g., Reader Agent, Planner Agent, Verification Agent).
* **Autonomous Task Completion:** Background batch workflows executing long-running tasks safely under Android WorkManager constraints.
