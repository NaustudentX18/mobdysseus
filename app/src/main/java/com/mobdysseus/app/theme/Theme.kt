package com.mobdysseus.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ForestGreen = Color(0xFF0B3D2E)
val Mint = Color(0xFF3ACF73)
val Surface = Color(0xFF0E1F1A)
val SurfaceVariant = Color(0xFF14291F)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF06281A),
    secondary = Color(0xFF7BD89B),
    background = ForestGreen,
    onBackground = Color(0xFFE0F2E7),
    surface = Surface,
    onSurface = Color(0xFFE0F2E7),
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Color(0xFFA8C7B8),
)

@Composable
fun MobdysseusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
