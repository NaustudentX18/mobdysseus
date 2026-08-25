# Mobdysseus — Developer Guide

## Hard rule: create files with bash heredocs, never the write/edit tool

The session `write`/`edit` tools create files as **symlinks into a temp overlay**
that gets cleaned up. Those symlinks then dangle and the file content **vanishes**
(and `read`/`edit` report "not found" or "file changed since read").

**Always create or replace files with a bash heredoc**, e.g.:

```bash
cat > app/src/main/java/com/mobdysseus/app/foo/Foo.kt <<'KOTLIN_EOF'
package com.mobdysseus.app.foo
// ...
KOTLIN_EOF
```

Then verify it is a real regular file, not a symlink:

```bash
[ -f app/src/main/java/com/mobdysseus/app/foo/Foo.kt ] \
  && [ ! -L app/src/main/java/com/mobdysseus/app/foo/Foo.kt ] \
  && wc -l app/src/main/java/com/mobdysseus/app/foo/Foo.kt
```

For small targeted edits to an existing file, use a Python string replace via a
heredoc (not the `edit` tool), or rewrite the whole file via heredoc.

## Guard

`scripts/guard.sh` fails if any symlink, `*.tmpdir`, `.l2s*`, or `*.tmp` leftover
exists under `app/src`, `docs`, or `licenses`. It runs in CI and should be run
before every commit:

```bash
bash scripts/guard.sh
```

## Build & test

```bash
./gradlew :app:testDebugUnitTest --console=plain --no-daemon   # unit tests
./gradlew :app:assembleDebug   --console=plain --no-daemon    # debug APK
./gradlew :app:assembleRelease --console=plain --no-daemon    # signed release
```

Release signing uses `release.keystore` (alias `mobdysseus`); the password is
`mobdysseus123` unless `MOBDYSSEUS_KEYSTORE_PASS` is set.

## Conventions

- Package root: `com.mobdysseus.app`. minSdk 30, compileSdk/targetSdk 36, JVM 17.
- JSON via `org.json` (no extra libs). Data stores persist to `Context.filesDir`
  as JSON; mirror `data/NotesStore.kt`.
- On-device LLM via `llmedge` (`io.github.aatricks:llmedge:0.4.7.2`, Apache-2.0).
- New features: add the store + screen, then wire the drawer in `ui/MainScreen.kt`
  and the store in `MainActivity.kt`.
