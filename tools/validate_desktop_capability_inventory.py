#!/usr/bin/env python3
"""Offline validator for the MOB-035 desktop capability inventory.

Run without a PC connection:
    python3 tools/validate_desktop_capability_inventory.py

To validate against a freshly exported desktop route listing (one entry per
line, as produced by ``dir C:\\...\\routes /b``):
    python3 tools/validate_desktop_capability_inventory.py --route-list routes.txt

Use ``--route-list -`` to supply that listing on stdin.  Route-list entries may
be bare names or begin with ``routes/``.  Python cache entries are ignored.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
INVENTORY = ROOT / "docs" / "desktop-capability-inventory.json"
BACKLOG = ROOT / "SWARM_BACKLOG.md"

VALID_CLASSIFICATIONS = {
    "native",
    "optional-network",
    "android-replacement",
    "impossible-standalone",
}

# Snapshot from C:\\Users\\jakem\\Projects\\odysseus\\routes on 2026-08-13.
# Keeping this list here makes the standard validation fully AFK and CI-safe.
EXPECTED_DESKTOP_ROUTES = {
    "routes/__init__.py",
    "routes/_validators.py",
    "routes/admin_wipe_routes.py",
    "routes/api_token_routes.py",
    "routes/assistant_routes.py",
    "routes/auth_routes.py",
    "routes/backup_routes.py",
    "routes/calendar_routes.py",
    "routes/chatgpt_subscription_routes.py",
    "routes/chat_helpers.py",
    "routes/chat_routes.py",
    "routes/cleanup_routes.py",
    "routes/codex_routes.py",
    "routes/compare_routes.py",
    "routes/contacts",
    "routes/contacts_routes.py",
    "routes/cookbook_helpers.py",
    "routes/cookbook_output.py",
    "routes/cookbook_routes.py",
    "routes/copilot_routes.py",
    "routes/device_flow.py",
    "routes/diagnostics_routes.py",
    "routes/document_helpers.py",
    "routes/document_routes.py",
    "routes/editor_draft_routes.py",
    "routes/email_helpers.py",
    "routes/email_pollers.py",
    "routes/email_routes.py",
    "routes/embedding_routes.py",
    "routes/emoji_routes.py",
    "routes/font_routes.py",
    "routes/gallery",
    "routes/gallery_helpers.py",
    "routes/gallery_routes.py",
    "routes/history",
    "routes/history_routes.py",
    "routes/hwfit_routes.py",
    "routes/mcp_routes.py",
    "routes/memory",
    "routes/memory_routes.py",
    "routes/model_routes.py",
    "routes/note_routes.py",
    "routes/personal_routes.py",
    "routes/prefs_routes.py",
    "routes/preset_routes.py",
    "routes/research",
    "routes/research_routes.py",
    "routes/search_routes.py",
    "routes/session_routes.py",
    "routes/shell_routes.py",
    "routes/signature_routes.py",
    "routes/skills_routes.py",
    "routes/stt_routes.py",
    "routes/task_routes.py",
    "routes/tts_routes.py",
    "routes/upload_routes.py",
    "routes/vault_routes.py",
    "routes/webhook_routes.py",
    "routes/workspace_routes.py",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--route-list",
        metavar="FILE|-",
        help="optional current desktop route listing; replaces the checked-in snapshot",
    )
    return parser.parse_args()


def normalize_route_list(raw: str) -> set[str]:
    routes: set[str] = set()
    for line in raw.splitlines():
        value = line.strip().replace("\\", "/")
        if not value or value in {"__pycache__", "routes/__pycache__"}:
            continue
        if not value.startswith("routes/"):
            value = f"routes/{value}"
        routes.add(value)
    return routes


def read_route_list(value: str) -> set[str]:
    raw = sys.stdin.read() if value == "-" else Path(value).read_text(encoding="utf-8")
    return normalize_route_list(raw)


def backlog_tickets(text: str) -> set[str]:
    return set(re.findall(r"^###\s+(MOB-\d{3})\s+—", text, flags=re.MULTILINE))


def main() -> int:
    args = parse_args()
    errors: list[str] = []

    try:
        inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"ERROR: cannot read valid JSON inventory {INVENTORY}: {exc}", file=sys.stderr)
        return 1

    entries = inventory.get("capabilities")
    if not isinstance(entries, list):
        errors.append("inventory.capabilities must be an array")
        entries = []

    desktop_modules: list[str] = []
    for index, entry in enumerate(entries):
        if not isinstance(entry, dict):
            errors.append(f"capabilities[{index}] must be an object")
            continue
        module = entry.get("desktop_module")
        classification = entry.get("classification")
        ticket = entry.get("mob_ticket")
        if not isinstance(module, str) or not module:
            errors.append(f"capabilities[{index}] has no desktop_module")
        else:
            desktop_modules.append(module)
        if classification not in VALID_CLASSIFICATIONS:
            errors.append(f"{module!r} has invalid classification {classification!r}")
        if not isinstance(ticket, str) or not re.fullmatch(r"MOB-\d{3}", ticket):
            errors.append(f"{module!r} has invalid mob_ticket {ticket!r}")

    duplicates = sorted({name for name in desktop_modules if desktop_modules.count(name) > 1})
    if duplicates:
        errors.append("duplicate desktop_module entries: " + ", ".join(duplicates))

    try:
        known_tickets = backlog_tickets(BACKLOG.read_text(encoding="utf-8"))
    except OSError as exc:
        errors.append(f"cannot read backlog {BACKLOG}: {exc}")
        known_tickets = set()
    unknown_tickets = sorted({entry.get("mob_ticket") for entry in entries if isinstance(entry, dict)} - known_tickets)
    if unknown_tickets:
        errors.append("MOB tickets absent from SWARM_BACKLOG.md: " + ", ".join(map(str, unknown_tickets)))

    expected = read_route_list(args.route_list) if args.route_list else EXPECTED_DESKTOP_ROUTES
    covered = set(desktop_modules)
    missing = sorted(expected - covered)
    if missing:
        errors.append("desktop route entries missing from inventory: " + ", ".join(missing))

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    source = args.route_list if args.route_list else "checked-in desktop route snapshot"
    print(
        f"OK: {len(entries)} inventory entries; {len(expected)} expected desktop routes covered; "
        f"{len(known_tickets)} MOB tickets discovered ({source})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
