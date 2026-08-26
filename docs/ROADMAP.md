# Mobdysseus — Roadmap

Everything below is driven by a single thesis: **no one unifies notes, documents, photos,
calendar, tasks, memory, on-device LLM chat and MCP tools into one offline, no-account,
no-cloud Android workspace.** Cloud AI (Notion AI, ChatGPT/Gemini memory, NotebookLM,
Galaxy AI) is powerful but cloud-bound, quota-limited, and offline-weak. Local-first tools
(Obsidian, Logseq, Anytype) are desktop-centric with AI bolted on as plugins and weak mobile
capture. That open lane is ours.

Each item lists the **market gap** it fills so every build pulls its weight.

---

## Phase 0 · The Data Plane (the privacy moat)

*Foundations: on-device retrieval + provable privacy. Everything later hangs off this.*

| Feature | What it does | Market gap it fills | Effort |
|---|---|---|---|
| **Ask Your Data (on-device RAG)** | Semantic search + Q&A over *all* local stores (notes, docs, calendar, gallery) with source citations, using on-device embeddings (e.g. EmbeddingGemma) on the S25 NPU. | Cloud search cannot see local files; offline RAG apps are single-format (PDF-only). Nobody queries their whole life offline. | M |
| **Privacy Verdict** | Per-feature audit trail: which data touched a model, whether it ever left the device, on-device vs cloud confidence. | "No cloud" is currently a claim, not proof. Google markets *cloud* confidentiality; Galaxy AI silently ships data unless disabled. A verifiable privacy audit is an unclaimed trust anchor. | S |
| **One-Tap Capture** | Voice / clipboard / photo → structured note from the share sheet, on-device STT + OCR, auto-tagged. | Capture is the #1 weakness of desktop-first local tools; nobody fuses it into a private graph on Android. | M |
| **Offline OCR photo search** | Index gallery EXIF/date/OCR-text/faces locally; ask "show the receipt photo." | Google Photos searches in the cloud; local-first apps don't index your existing photos. | M |

---

## Phase 1 · The Memory Plane

*Turn scattered data into a durable, browsable, *personal* model of the user.*

| Feature | Why | Gap it fills | Effort |
|---|---|---|---|
| **Mnemosyne — knowledge graph** | Entity/relation graph built from notes, docs, calendar, chat; browsable; feeds every feature's context. | ChatGPT/Gemini memory is an opaque cloud black box; local graph tools are dev-only. A polished consumer graph is unclaimed on Android. | M |
| **Context Docks** | Explicit, *visible* user-taught context ("I work 9–5", "allergic to X") injected before answers. | Kills the "creepy, it knows me" feeling by making personal context transparent and user-owned. | M |
| **Private Inbox** | On-device transcription + triage + drafting + daily digest for email and voice memos. | Email agents are enterprise/self-hosted or cloud; meeting transcription is a point tool. No consumer offline triage exists. | M |
| **Self-hosted Sync (Pi/NAS)** | LAN-local sync + bigger model/RAG with a user-owned Pi/NAS. Still no cloud account. | Local-first is RAM/thermal-capped on a phone; cloud-first is account-bound. A *user-owned* middle tier is unclaimed — and beats Obsidian's \$3–8/mo paid cloud. | L |

---

## Phase 2 · MCP Trust & Agents

*Make the on-device + MCP story safe and genuinely agentic.*

| Feature | Why | Gap it fills | Effort |
|---|---|---|---|
| **Phone-as-MCP-Server** | Expose the phone's own data (contacts, calendar, files, clipboard) to the *local* agent as a private tool server, gated by per-tool consent. | Phone MCP today exposes the phone to *remote* agents (a consent nightmare). Nothing keeps data local with an OS-level permission model. | L |
| **Permission-Consent Gate** | Every side-effecting tool call needs an explicit inline tap; read tools auto-approve under a session trust. | Tool poisoning / prompt injection is MCP's #1 security problem; clients treat tool calls as implicit. | M |
| **Trust Check on Connect** | Pre-flight audit of a new server (scheme, capabilities, origin) + a Trust score before use. | Nobody lets you vet an MCP server before you paste-and-pray. | M |
| **Agent Loops / Steward** | Chain tools into multi-step tasks with progress + stop + per-step log. | Desktop can chain tools; nobody productizes it as a controllable mobile loop. | L |
| **MCP App Store** | Curated catalog of self-hosted servers, one-tap Connect. | Catalogs are desktop/CLI JSON-paste; no phone-native, permission-aware installer exists. | M |

---

## Phase 3 · Galaxy S25-Native

*Lean into One UI surfaces and the S25's Snapdragon 8 Elite that generic AI apps ignore.*

| Feature | Why the S25 | Gap | Effort |
|---|---|---|---|
| **Private Voice Assistant** | Wake-word on-device STT → local LLM → local TTS, answering from your data, airplane-mode proof. | Galaxy/Gemini voice is cloud-bound; offline voice that reads *your* local data is the flagship "private" demo. | L |
| **Live widgets & Edge panel** | Now Bar / home-screen task+capture widget, one-swipe Edge panel AI actions. | Samsung's own Now Brief is fixed-schema; third-party AI apps don't live in the widget/edge layer. | S |
| **DeX multi-pane workspace** | Side-by-side notes/calendar/memory/chat when DeX/freeform is detected. | Cloud AI apps treat DeX as one stretched phone pane. | M |
| **Health copilot (local)** | Trend/narrative summaries from on-device Samsung Health data. | Samsung Health shows raw charts, no narrative; cloud health AI requires exporting sensitive data. | M |
| **Battery-aware inference** | Schedule indexing/summaries to charging/battery-safe windows, cooperating with One UI power policies. | Local-LLM apps ignore Android's battery policies and get killed or burn battery. | M |

---

## Where to start (suggested shipping slices)

1. **Ship Phase 0 core now:** Ask Your Data + Privacy Verdict (both shippable on the current GGUF/on-device stack), then One-Tap Capture + OCR.
2. **Then Phase 1** memory graph + self-hosted sync — the durable moat.
3. **Then Phase 2** trust + consent spine (permission gate, trust check, phone-as-server), then agent loops.
4. **Layer Phase 3** S25-native surfaces on top as they land.

**North star:** a personal, verifiable, fully offline AI data plane on the Galaxy S25 — with an optional user-owned server tier for power, and a trust-first MCP gateway. No cloud assistant can reach the personal data plane; no local app yet unifies it into a workspace.
