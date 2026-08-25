# Mobdysseus — Smoke Test

Install `dist/Mobdysseus-v0.7.0.apk` (signed release) or the debug APK and walk this list on a Samsung Galaxy S25.

## 0. Install & signature
- [ ] `apksigner verify --print-certs dist/Mobdysseus-v0.7.0.apk` shows `CN=Mobdysseus`.
- [ ] SHA-256 matches the recorded value in `dist/`.

## 1. Launch & navigation
- [ ] App opens to the **Chat** tab, dark forest-green theme, no crash.
- [ ] Drawer (☰) lists all sections: **Chat, Notes, Documents, Tasks, Calendar, Memory, Gallery, Research, Cookbook, MCP Tools, Settings**.
- [ ] Each drawer entry opens the matching screen with the correct title.

## 2. Chat (remote)
- [ ] **Settings → Cloud API**: tap a preset (e.g. **Ollama (local)**); Base URL + Model fill in.
- [ ] Enter API key if needed; **Test connection** reports *Reachable* or *Unreachable*.
- [ ] Save; reopen Settings → values persist.
- [ ] Back on **Chat**, send a message → reply streams token-by-token (or shows a clear provider error if unreachable).

## 3. Chat (on-device)
- [ ] **Settings → On-device:** pick a GGUF model; first message triggers a one-time HuggingFace download, then runs offline.
- [ ] Send a follow-up message → multi-turn context is retained (recent history).
- [ ] Kill and reopen → conversation history persists in **ChatStore**.

## 4. Notes / Tasks / Documents
- [ ] Add / edit / delete an item in each; data persists across app restarts.
- [ ] Markdown renders in chat/notes.

## 5. Calendar
- [ ] Month grid renders (Monday-first); days with events show a dot.
- [ ] **+** adds an event (title/date/notes); it appears on its day; delete works; persists.

## 6. Memory
- [ ] Add a knowledge entry; **search** finds it by keyword; delete works; persists.

## 7. Gallery
- [ ] Grants photo permission on first open; device images appear in a 3-column grid.
- [ ] Tapping an image opens a full-screen viewer.

## 8. Research
- [ ] Enter a query → **Search** returns result cards (title/url/snippet).
- [ ] Offline / empty-result shows a friendly message.

## 9. Cookbook
- [ ] Shows detected hardware (SoC, RAM) and ranked recommended models.

## 10. MCP Tools
- [ ] **+** add a server (name + URL, e.g. a Pi bridge at `http://192.168.4.44:8101`).
- [ ] Open a server → **tools list** loads; **Run** a tool → streamed result renders.
- [ ] Delete a server; list persists.

## 11. Settings → About
- [ ] About text: Mobdysseus v0.7.0, AGPL-3.0-or-later, "not affiliated with Odysseus".

## 12. Persistence & battery
- [ ] Kill + reopen app → all stores (notes/tasks/documents/calendar/memory/servers) persist.
- [ ] On-device inference runs inside the foreground service (wakelock held, notification visible).

**Known limits:** Email (B5) and server-hosted Agents (B6) are not yet shipped; GPU/NPU offload is a later hardening step; on-device model quality depends on the chosen GGUF.
