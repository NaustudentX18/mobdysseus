# Mobdysseus — Smoke Test

Install `dist/Mobdysseus-v0.1.0.apk` (or the debug APK) and walk this list.

## 1. Launch
- [ ] App opens to the **Chat** tab, dark forest-green theme, no crash.
- [ ] Bottom bar shows **Chat / Notes / Settings**.

## 2. Settings → provider
- [ ] Open **Settings**; tap a preset (e.g. **Ollama (local)**); Base URL and
      Model fill in.
- [ ] Enter an API key if needed; tap **Save**; reopen Settings → values persist.

## 3. Chat
- [ ] Back on **Chat**, type a message and tap **Send**.
- [ ] The reply streams in token-by-token (or shows a clear provider error if
      the endpoint/key is unreachable — expected offline).
- [ ] Messages bubble: user right (green), assistant left (dark).

## 4. Notes
- [ ] Open **Notes**; tap **+**; enter a title + body; **Save**; note appears.
- [ ] Tap a note to edit; **Delete** removes it.
- [ ] Kill and reopen the app → notes persist.

## 5. Signature (release only)
- [ ] `apksigner verify --print-certs dist/Mobdysseus-v0.1.0.apk` shows
      `CN=Mobdysseus`.

**Known limits (v0.1.0):** no on-device model, no offline chat, no server
pairing yet — those are Phase 2+.
