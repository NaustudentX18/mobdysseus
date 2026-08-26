"""Contract tests for Mobdysseus v2 Comprehensive Settings & Local Runtime Fallbacks."""

from __future__ import annotations

from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
WORKSPACE_CORE = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/core/Workspace.kt"
PERSISTENCE_DOMAIN = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/persistence/WorkspacePersistenceDomain.kt"
DATABASE_FILE = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/persistence/database/MobdysseusDatabase.kt"
WORKSPACE_ENTITIES = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/persistence/database/WorkspaceEntities.kt"
RUNTIME_FILE = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/LocalModelRuntime.kt"
MORE_SCREEN = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/feature/MoreScreen.kt"


def test_core_settings_and_persistence_cover_full_inference_and_voice_knobs() -> None:
    core_code = WORKSPACE_CORE.read_text(encoding="utf-8")
    persistence_code = PERSISTENCE_DOMAIN.read_text(encoding="utf-8")
    entities_code = WORKSPACE_ENTITIES.read_text(encoding="utf-8")

    expected_fields = [
        "temperature",
        "topP",
        "topK",
        "maxTokens",
        "systemPrompt",
        "ragTopK",
        "voiceAutoSpeak",
        "voiceSpeechRate",
        "voiceSpeechPitch",
        "biometricLockEnabled",
        "notificationsEnabled",
        "markdownPreviewDefault",
        "autoSaveDrafts",
    ]

    for field in expected_fields:
        assert field in core_code, f"Missing {field} in core Workspace.MobileSettings"
        assert field in persistence_code, f"Missing {field} in WorkspaceSettingsRecord"
        assert field in entities_code, f"Missing {field} in WorkspaceEntity"


def test_room_v5_migration_script_adds_all_settings_columns() -> None:
    db_code = DATABASE_FILE.read_text(encoding="utf-8")
    assert "version = 5" in db_code
    assert "MIGRATION_4_5" in db_code
    assert "ALTER TABLE workspace ADD COLUMN temperature REAL NOT NULL DEFAULT 0.7" in db_code
    assert "ALTER TABLE workspace ADD COLUMN topP REAL NOT NULL DEFAULT 0.9" in db_code
    assert "ALTER TABLE workspace ADD COLUMN topK INTEGER NOT NULL DEFAULT 32" in db_code
    assert "ALTER TABLE workspace ADD COLUMN ragTopK INTEGER NOT NULL DEFAULT 3" in db_code
    assert "ALTER TABLE workspace ADD COLUMN voiceAutoSpeak INTEGER NOT NULL DEFAULT 0" in db_code


def test_runtime_implements_gpu_to_cpu_fallback_and_passes_custom_sampler() -> None:
    runtime_code = RUNTIME_FILE.read_text(encoding="utf-8")
    assert "Backend.GPU()" in runtime_code
    assert "Backend.CPU()" in runtime_code
    assert "SamplerConfig" in runtime_code
    assert "topK = topK" in runtime_code
    assert "temperature = temperature" in runtime_code


def test_more_screen_exposes_interactive_settings_cards() -> None:
    more_code = MORE_SCREEN.read_text(encoding="utf-8")
    assert "InferenceSettingsCard" in more_code
    assert "RagSettingsCard" in more_code
    assert "VoiceSettingsCard" in more_code
    assert "EditorSettingsCard" in more_code
