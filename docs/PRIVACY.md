# Mobdysseus Privacy & Zero-Trust Manifesto

> **Principle:** Computational Sovereignty on Edge Hardware.

---

## 🔒 1. Zero-Network Default

Mobdysseus operates on a strict **zero-network assumption**:
* **Airplane-Mode Operational:** The app requires zero network connection for full functionality.
* **No Unsolicited Egress:** No background telemetry, crash reports, heartbeat pings, or analytics beacons exist in this repository.
* **Consented External Actions:** Network access is only used when the user explicitly requests an external service (e.g. downloading a model from HuggingFace or sending an optional API query to a configured provider).

---

## 🛡️ 2. Hardware-Backed Encryption

* **SQLCipher at Rest:** All chat histories, notes, tasks, long-term memories, and metadata are encrypted inside SQLite using 256-bit AES-GCM via SQLCipher.
* **Android Keystore Root of Trust:** Encryption keys never touch persistent plaintext storage. Master keys are generated inside the Android hardware Security Module (TEE / StrongBox).
* **Encrypted Backups:** User exports (`.mobdbak`) are encrypted using 100,000 rounds of PBKDF2-HMAC-SHA256 and authenticated AES-GCM. Passphrases are never stored or recoverable.

---

## 🧠 3. Memory Governance & Anti-Poisoning

* **Mnemosyne Review Gate:** When an LLM suggests storing a memory during conversation, it is placed in an uncommitted quarantine queue.
* **Prompt-Injection Resistance:** Memories are never written into long-term storage without an explicit, physical confirmation tap from the user, preventing malicious documents or prompt injections from hijacking agent memory.

---

## 📜 4. Data Retention & Scoped Deletion

* **Local-Only Wipe:** The user can instantly wipe individual modules (Chats, Notes, Brain Memories, Gallery, Models) or perform a complete cryptographic zeroization from `MoreScreen`.
* **Zero-Copy Scoped Storage:** File imports strictly duplicate content into the app-private sandbox (`context.filesDir`), ensuring external apps cannot modify or trace files once imported.
