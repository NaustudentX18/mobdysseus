# Mobdysseus contributor instructions

## Product contract

- Native Android only: Kotlin/Compose. Do not add a WebView implementation.
- Standalone core must work in airplane mode without a PC, Pi, API key or server.
- Use `PARITY.md` and `SWARM_BACKLOG.md` as the completion contract.
- Desktop-only actions must have an explicit permissioned Android equivalent; never hide a remote dependency.
- Preserve the application ID unless a migration plan is approved, so installed data survives upgrades.

## Swarm ownership

- Claim one MOB ticket and its module before editing.
- Avoid app-shell, navigation, schema or Gradle edits unless the ticket explicitly owns them.
- Do not edit another active agent's files. Coordinate shared-contract changes first.
- New capabilities must go through typed interfaces and the central approval/permission policy.

## Privacy and safety

- No private content, prompts, documents, model output, tokens or secrets in logs.
- No broad storage permission, arbitrary subprocesses, unrestricted filesystem paths or silent external actions.
- External writes, sharing, sending and account access require visible user confirmation.
- Store imported files and models in app-private storage; validate type, size and integrity.

## Build verification

The authoritative Android build currently runs on the paired Windows PC:

```text
C:\Users\jakem\Projects\odysseus-mobile-native\gradlew.bat --no-daemon \
  -p C:\Users\jakem\Projects\odysseus-mobile-native assembleDebug
```

When syncing from Pi, exclude `.gradle`, `.kotlin`, `app/build`, `local.properties`,
heap dumps and the wrapper JAR already in use by Windows. Do not call a ticket complete
without a green compile and ticket-proportionate tests.
