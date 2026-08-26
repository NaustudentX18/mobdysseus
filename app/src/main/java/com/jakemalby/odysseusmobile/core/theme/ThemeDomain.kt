package com.jakemalby.odysseusmobile.core.theme

/**
 * Supported themes in Mobdysseus.
 * Stored in Workspace settings and applied across all screens.
 */
enum class AppTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryHex: Long,
    val backgroundHex: Long,
) {
    OBSIDIAN_CORAL(
        id = "OBSIDIAN_CORAL",
        displayName = "Obsidian Coral",
        description = "Classic Mobdysseus aesthetic with dark obsidian and coral accents",
        primaryHex = 0xFFE06C75,
        backgroundHex = 0xFF111318,
    ),
    CYBERPUNK_NEON(
        id = "CYBERPUNK_NEON",
        displayName = "Cyberpunk Neon",
        description = "OLED pitch black with hyper-vibrant cyan and magenta neon",
        primaryHex = 0xFF00F0FF,
        backgroundHex = 0xFF060709,
    ),
    MIDNIGHT_NAVY(
        id = "MIDNIGHT_NAVY",
        displayName = "Midnight Navy",
        description = "Deep indigo night sky with electric blue highlights",
        primaryHex = 0xFF4D96FF,
        backgroundHex = 0xFF0A0E17,
    ),
    SOLARIZED_AMBER(
        id = "SOLARIZED_AMBER",
        displayName = "Solarized Amber",
        description = "Warm espresso dark palette with rich amber/gold tones",
        primaryHex = 0xFFF5A623,
        backgroundHex = 0xFF14120E,
    ),
    FOREST_MATRIX(
        id = "FOREST_MATRIX",
        displayName = "Forest Matrix",
        description = "High-contrast terminal dark with vibrant emerald green",
        primaryHex = 0xFF00E676,
        backgroundHex = 0xFF070F0A,
    ),
    MONOKAI_VAPOR(
        id = "MONOKAI_VAPOR",
        displayName = "Monokai Vapor",
        description = "Refined charcoal theme with neon purple and warm orange accents",
        primaryHex = 0xFFAB87FF,
        backgroundHex = 0xFF1E1F22,
    ),
    DYNAMIC_MATERIAL(
        id = "DYNAMIC_MATERIAL",
        displayName = "Material You",
        description = "Adaptive system colors extracted from device wallpaper",
        primaryHex = 0xFF80D4FF,
        backgroundHex = 0xFF111418,
    );

    companion object {
        fun fromId(id: String?): AppTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OBSIDIAN_CORAL
        }
    }
}

/**
 * Storage-neutral, non-Android color representation for testing and cross-platform safety.
 */
data class ThemeColorPalette(
    val primary: Long,
    val onPrimary: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    val success: Long,
    val error: Long,
    val isDark: Boolean = true,
)

fun paletteFor(theme: AppTheme): ThemeColorPalette = when (theme) {
    AppTheme.OBSIDIAN_CORAL -> ThemeColorPalette(
        primary = 0xFFE06C75,
        onPrimary = 0xFF111318,
        background = 0xFF111318,
        onBackground = 0xFFE7E9F0,
        surface = 0xFF1B1E25,
        onSurface = 0xFFE7E9F0,
        surfaceVariant = 0xFF242833,
        onSurfaceVariant = 0xFFABB1C0,
        outline = 0xFF343946,
        success = 0xFF7BC99A,
        error = 0xFFFF5370,
    )
    AppTheme.CYBERPUNK_NEON -> ThemeColorPalette(
        primary = 0xFF00F0FF,
        onPrimary = 0xFF060709,
        background = 0xFF060709,
        onBackground = 0xFFE0F7FA,
        surface = 0xFF0F111A,
        onSurface = 0xFFE0F7FA,
        surfaceVariant = 0xFF171A29,
        onSurfaceVariant = 0xFF80DEEA,
        outline = 0xFF1E2640,
        success = 0xFF00E676,
        error = 0xFFFF007F,
    )
    AppTheme.MIDNIGHT_NAVY -> ThemeColorPalette(
        primary = 0xFF4D96FF,
        onPrimary = 0xFF0A0E17,
        background = 0xFF0A0E17,
        onBackground = 0xFFF0F4FC,
        surface = 0xFF121826,
        onSurface = 0xFFF0F4FC,
        surfaceVariant = 0xFF1C2436,
        onSurfaceVariant = 0xFF9FB2D9,
        outline = 0xFF243049,
        success = 0xFF6BCB77,
        error = 0xFFFF6B6B,
    )
    AppTheme.SOLARIZED_AMBER -> ThemeColorPalette(
        primary = 0xFFF5A623,
        onPrimary = 0xFF14120E,
        background = 0xFF14120E,
        onBackground = 0xFFFDF6E3,
        surface = 0xFF1E1B15,
        onSurface = 0xFFFDF6E3,
        surfaceVariant = 0xFF2B261E,
        onSurfaceVariant = 0xFFC9B9A6,
        outline = 0xFF3E362A,
        success = 0xFF859900,
        error = 0xFFDC322F,
    )
    AppTheme.FOREST_MATRIX -> ThemeColorPalette(
        primary = 0xFF00E676,
        onPrimary = 0xFF070F0A,
        background = 0xFF070F0A,
        onBackground = 0xFFE0F2E9,
        surface = 0xFF0E1A11,
        onSurface = 0xFFE0F2E9,
        surfaceVariant = 0xFF16291C,
        onSurfaceVariant = 0xFF81C784,
        outline = 0xFF1E3B27,
        success = 0xFF00E676,
        error = 0xFFFF5252,
    )
    AppTheme.MONOKAI_VAPOR -> ThemeColorPalette(
        primary = 0xFFAB87FF,
        onPrimary = 0xFF1E1F22,
        background = 0xFF1E1F22,
        onBackground = 0xFFF8F8F2,
        surface = 0xFF27282D,
        onSurface = 0xFFF8F8F2,
        surfaceVariant = 0xFF32343D,
        onSurfaceVariant = 0xFFB0B2C0,
        outline = 0xFF434552,
        success = 0xFFA6E22E,
        error = 0xFFF92672,
    )
    AppTheme.DYNAMIC_MATERIAL -> ThemeColorPalette(
        primary = 0xFF80D4FF,
        onPrimary = 0xFF00354E,
        background = 0xFF111418,
        onBackground = 0xFFE1E2E8,
        surface = 0xFF191C20,
        onSurface = 0xFFE1E2E8,
        surfaceVariant = 0xFF23262B,
        onSurfaceVariant = 0xFFC2C7CF,
        outline = 0xFF3B4048,
        success = 0xFF6CD89B,
        error = 0xFFFFB4AB,
    )
}
