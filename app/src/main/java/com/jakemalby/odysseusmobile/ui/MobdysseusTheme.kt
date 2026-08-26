package com.jakemalby.odysseusmobile.ui

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.jakemalby.odysseusmobile.core.theme.AppTheme
import com.jakemalby.odysseusmobile.core.theme.ThemeColorPalette
import com.jakemalby.odysseusmobile.core.theme.paletteFor

/**
 * Mobdysseus UI theme colors providing quick semantic access across all screens.
 */
data class MobdysseusColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceRaised: Color,
    val onSurfaceVariant: Color,
    val border: Color,
    val success: Color,
    val error: Color,
    val activeTheme: AppTheme,
)

val LocalMobdysseusColors = staticCompositionLocalOf {
    val defaultPalette = paletteFor(AppTheme.OBSIDIAN_CORAL)
    MobdysseusColors(
        primary = Color(defaultPalette.primary),
        onPrimary = Color(defaultPalette.onPrimary),
        background = Color(defaultPalette.background),
        onBackground = Color(defaultPalette.onBackground),
        surface = Color(defaultPalette.surface),
        onSurface = Color(defaultPalette.onSurface),
        surfaceRaised = Color(defaultPalette.surfaceVariant),
        onSurfaceVariant = Color(defaultPalette.onSurfaceVariant),
        border = Color(defaultPalette.outline),
        success = Color(defaultPalette.success),
        error = Color(defaultPalette.error),
        activeTheme = AppTheme.OBSIDIAN_CORAL,
    )
}

object MobdysseusThemeColors {
    val current: MobdysseusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMobdysseusColors.current
}

@Composable
fun MobdysseusAppTheme(
    theme: AppTheme = AppTheme.OBSIDIAN_CORAL,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val palette = paletteFor(theme)
    
    val colorScheme: ColorScheme = if (theme == AppTheme.DYNAMIC_MATERIAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching { dynamicDarkColorScheme(context) }.getOrElse {
            darkColorScheme(
                primary = Color(palette.primary),
                onPrimary = Color(palette.onPrimary),
                background = Color(palette.background),
                onBackground = Color(palette.onBackground),
                surface = Color(palette.surface),
                onSurface = Color(palette.onSurface),
                surfaceVariant = Color(palette.surfaceVariant),
                onSurfaceVariant = Color(palette.onSurfaceVariant),
                outline = Color(palette.outline),
                error = Color(palette.error),
            )
        }
    } else {
        darkColorScheme(
            primary = Color(palette.primary),
            onPrimary = Color(palette.onPrimary),
            background = Color(palette.background),
            onBackground = Color(palette.onBackground),
            surface = Color(palette.surface),
            onSurface = Color(palette.onSurface),
            surfaceVariant = Color(palette.surfaceVariant),
            onSurfaceVariant = Color(palette.onSurfaceVariant),
            outline = Color(palette.outline),
            error = Color(palette.error),
        )
    }

    val mobdysseusColors = MobdysseusColors(
        primary = colorScheme.primary,
        onPrimary = colorScheme.onPrimary,
        background = colorScheme.background,
        onBackground = colorScheme.onBackground,
        surface = colorScheme.surface,
        onSurface = colorScheme.onSurface,
        surfaceRaised = colorScheme.surfaceVariant,
        onSurfaceVariant = colorScheme.onSurfaceVariant,
        border = colorScheme.outline,
        success = Color(palette.success),
        error = colorScheme.error,
        activeTheme = theme,
    )

    CompositionLocalProvider(LocalMobdysseusColors provides mobdysseusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
