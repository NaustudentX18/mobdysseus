"""Dependency-free checks for the v0 encrypted-workspace migration contract."""

from __future__ import annotations

import json
from pathlib import Path


PROJECT = Path(__file__).resolve().parents[1]
PERSISTENCE = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/persistence"
FIXTURE = PROJECT / "tools/fixtures/workspace-v0.json"


def test_v0_fixture_has_exact_legacy_shape_and_referential_integrity() -> None:
    payload = json.loads(FIXTURE.read_text(encoding="utf-8"))
    assert set(payload) == {"active", "conversations", "notes", "tasks", "memories", "gallery", "settings"}
    assert payload["active"] in {conversation["id"] for conversation in payload["conversations"]}
    assert set(payload["settings"]) == {"recipe", "localOnly", "compact"}
    assert set(payload["conversations"][0]["messages"][0]) == {"id", "author", "text", "mine", "created"}
    assert set(payload["notes"][0]) == {"id", "title", "body", "updated"}
    assert set(payload["tasks"][0]) == {"id", "title", "done"}
    assert set(payload["memories"][0]) == {"id", "text", "created"}
    assert set(payload["gallery"][0]) == {"id", "name", "path", "created"}


def test_migration_codec_covers_each_v0_field_and_is_storage_neutral() -> None:
    code = (PERSISTENCE / "V0WorkspaceMigrationCodec.kt").read_text(encoding="utf-8")
    for legacy_key in ("conversations", "active", "notes", "tasks", "memories", "gallery", "settings"):
        assert f'"{legacy_key}"' in code
    assert "LegacyMigrationResult.Rejected" in code
    assert "WorkspaceRepository" in code  # migration documentation names the write boundary
    assert "SharedPreferences" not in code
    assert "SecureWorkspaceStorage" not in code
    assert "MainActivity" not in code


def test_domain_requires_current_schema_and_active_conversation_integrity() -> None:
    domain = (PERSISTENCE / "WorkspacePersistenceDomain.kt").read_text(encoding="utf-8")
    assert "schemaVersion == CURRENT_SCHEMA_VERSION" in domain
    assert "conversations.any { it.id == activeConversationId }" in domain
    assert "interface WorkspaceRepository" in domain
    assert "suspend fun replace(snapshot: WorkspaceSnapshot)" in domain
