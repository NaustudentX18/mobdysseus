#!/usr/bin/env python3
"""Enforce the MOB-001 Kotlin dependency directions.

Architecture after extraction:

    app shell -> navigation -> feature -> core
                          -> core

* ``core`` and the platform-neutral ``capability`` policy are domain code and cannot depend on navigation, app
  shell, or a feature.
* ``navigation`` may depend only on core and feature public entry points.
* a ``feature/<name>`` may depend on core and its own feature package, never
  on another feature implementation or navigation.
* the app shell may depend on navigation and core contracts, but not feature
  implementation packages.

The guard checks imports and fully-qualified project-package references.  It
also ensures that MainActivity is only shell wiring: feature screen composable
definitions (for example ``ChatScreen``) must be extracted from it.

Run from any directory:
    python3 tools/check_dependency_boundaries.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
KOTLIN_ROOT = ROOT / "app" / "src" / "main" / "java"
PACKAGE_ROOT = "com.jakemalby.odysseusmobile"
MAIN_ACTIVITY = KOTLIN_ROOT / "com" / "jakemalby" / "odysseusmobile" / "MainActivity.kt"

IMPORT_RE = re.compile(r"^\s*import\s+([\w.]+)", re.MULTILINE)
PROJECT_REFERENCE_RE = re.compile(
    r"\bcom\.jakemalby\.odysseusmobile\.(?:core|navigation|feature|features)(?:\.[A-Za-z_]\w*)+"
)
SCREEN_FUNCTION_RE = re.compile(
    r"^\s*(?:private\s+|internal\s+|public\s+)?(?:suspend\s+)?fun\s+([A-Za-z_]\w*Screen)\s*\(",
    re.MULTILINE,
)
SHELL_WIRING_MARKERS = {
    "MainActivity declaration": re.compile(r"\bclass\s+MainActivity\b"),
    "Compose content setup": re.compile(r"\bsetContent\s*\{"),
}


def layer_for(path: Path) -> tuple[str, str | None]:
    """Return architecture layer and feature name, if any, based on source path."""
    relative = path.relative_to(KOTLIN_ROOT).as_posix().split("/")
    try:
        root_index = relative.index("odysseusmobile")
    except ValueError:
        return "external", None
    tail = relative[root_index + 1 :]
    if not tail:
        return "shell", None
    if tail[0] in {"core", "capability"}:
        return "core", None
    if tail[0] == "navigation":
        return "navigation", None
    if tail[0] in {"feature", "features"} and len(tail) > 1:
        return "feature", tail[1]
    return "shell", None


def target_for(reference: str) -> tuple[str, str | None]:
    suffix = reference.removeprefix(PACKAGE_ROOT + ".")
    segments = suffix.split(".")
    if segments[0] in {"core", "capability"}:
        return "core", None
    if segments[0] == "navigation":
        return "navigation", None
    if segments[0] in {"feature", "features"}:
        return "feature", segments[1] if len(segments) > 1 else None
    return "shell", None


def allowed(source_layer: str, source_feature: str | None, target_layer: str, target_feature: str | None) -> bool:
    if source_layer == "core":
        return target_layer == "core"
    if source_layer == "navigation":
        return target_layer in {"core", "feature"}
    if source_layer == "feature":
        return target_layer == "core" or (target_layer == "feature" and source_feature == target_feature)
    # Shell owns composition only.  It deliberately reaches navigation, never
    # individual feature implementations after MOB-001 extraction.
    return target_layer in {"core", "navigation", "shell"}


def references_in(text: str) -> set[str]:
    imports = set(IMPORT_RE.findall(text))
    qualified = set(PROJECT_REFERENCE_RE.findall(text))
    return {item for item in imports | qualified if item.startswith(PACKAGE_ROOT + ".")}


def main() -> int:
    if not KOTLIN_ROOT.is_dir():
        print(f"ERROR: Kotlin source root not found: {KOTLIN_ROOT}", file=sys.stderr)
        return 1

    failures: list[str] = []
    files = sorted(KOTLIN_ROOT.rglob("*.kt"))
    for source in files:
        source_layer, source_feature = layer_for(source)
        text = source.read_text(encoding="utf-8")
        for reference in sorted(references_in(text)):
            target_layer, target_feature = target_for(reference)
            if allowed(source_layer, source_feature, target_layer, target_feature):
                continue
            location = source.relative_to(ROOT)
            failures.append(
                f"{location}: {source_layer} may not reference {reference} "
                f"({target_layer}{'/' + target_feature if target_feature else ''}). "
                "Move the shared contract to core, expose a feature entry point through navigation, "
                "or remove the cross-feature dependency."
            )

    if not MAIN_ACTIVITY.is_file():
        failures.append(f"{MAIN_ACTIVITY.relative_to(ROOT)} is missing; shell wiring must live in MainActivity.")
    else:
        main_text = MAIN_ACTIVITY.read_text(encoding="utf-8")
        for label, marker in SHELL_WIRING_MARKERS.items():
            if not marker.search(main_text):
                failures.append(
                    f"{MAIN_ACTIVITY.relative_to(ROOT)} is missing {label}; "
                    "keep activity and Compose/NavHost shell wiring in MainActivity."
                )
        screen_functions = SCREEN_FUNCTION_RE.findall(main_text)
        if screen_functions:
            failures.append(
                f"{MAIN_ACTIVITY.relative_to(ROOT)} still defines feature screen composables: "
                f"{', '.join(name.strip() for name in screen_functions)}. "
                "Extract each screen into feature/<name>/ and leave only activity, theme/provider setup, and NavHost wiring."
            )

    if failures:
        print("MOB-001 dependency-boundary check FAILED:", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print(f"OK: MOB-001 dependency boundaries pass across {len(files)} Kotlin file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
