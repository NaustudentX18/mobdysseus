#!/usr/bin/env bash
# Guard: fail if any tracked source area contains symlinks or temp-overlay
# leftovers from the harness write/edit tools. Run in CI and before commits.
set -euo pipefail
cd "$(dirname "$0")/.."

bad=0
echo "guard: checking app/src, docs, licenses for symlinks / temp-overlay leftovers"

while IFS= read -r f; do
  echo "  FAIL (symlink): $f"
  bad=1
done < <(find app/src docs licenses -type l 2>/dev/null | grep -v '\.gradle' || true)

while IFS= read -r d; do
  echo "  FAIL (tmpdir): $d"
  bad=1
done < <(find app/src docs licenses -type d -name '*.tmpdir' 2>/dev/null || true)

while IFS= read -r f; do
  echo "  FAIL (.l2s/.tmp): $f"
  bad=1
done < <(find app/src docs licenses \( -name '.l2s*' -o -name '*.tmp' \) -type f 2>/dev/null || true)

if [ "$bad" -ne 0 ]; then
  echo "guard: FAILED"
  exit 1
fi
echo "guard: OK"
