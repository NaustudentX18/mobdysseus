"""Contract tests for Mobdysseus v2 Theme Engine and Color Schemes."""

from __future__ import annotations

from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
THEME_DOMAIN = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/core/theme/ThemeDomain.kt"
THEME_UI = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/ui/MobdysseusTheme.kt"
PERSISTENCE_DOMAIN = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/persistence/WorkspacePersistenceDomain.kt"
DATABASE_FILE = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/persistence/database/MobdysseusDatabase.kt"
WORKSPACE_ENTITIES = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/persistence/database/WorkspaceEntities.kt"


def test_theme_domain_defines_all_core_themes_and_palettes() -> None:
    code = THEME_DOMAIN.read_text(encoding="utf-8")
    expected_themes = [
        "OBSIDIAN_CORAL",
        "CYBERPUNK_NEON",
        "MIDNIGHT_NAVY",
        "SOLARIZED_AMBER",
        "FOREST_MATRIX",
        "MONOKAI_VAPOR",
        "DYNAMIC_MATERIAL",
    ]
    for theme in expected_themes:
        assert theme in code, f"Missing theme enum {theme}"
    assert "data class ThemeColorPalette" in code
    assert "fun paletteFor(theme: AppTheme): ThemeColorPalette" in code


def test_theme_persistence_contract_and_room_migration() -> None:
    persistence_code = PERSISTENCE_DOMAIN.read_text(encoding="utf-8")
    assert 'val theme: String = "OBSIDIAN_CORAL"' in persistence_code

    entities_code = WORKSPACE_ENTITIES.read_text(encoding="utf-8")
    assert 'val theme: String = "OBSIDIAN_CORAL"' in entities_code

    db_code = DATABASE_FILE.read_text(encoding="utf-8")
    assert "version = 4" in db_code
    assert "MIGRATION_3_4" in db_code
    assert "ALTER TABLE workspace ADD COLUMN theme TEXT NOT NULL DEFAULT 'OBSIDIAN_CORAL'" in db_code


def test_theme_ui_layer_provides_composition_local_and_material_theme() -> None:
    ui_code = THEME_UI.read_text(encoding="utf-8")
    assert "LocalMobdysseusColors" in ui_code
    assert "MobdysseusAppTheme" in ui_code
    assert "dynamicDarkColorScheme" in ui_code
