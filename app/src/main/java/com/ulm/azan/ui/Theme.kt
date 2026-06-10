package com.ulm.azan.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Brand palette and decorative brushes for the modern Islamic look. */
object Brand {
    val EmeraldDeep = Color(0xFF06443A)
    val Emerald = Color(0xFF0B6B57)
    val EmeraldLight = Color(0xFF2E8B73)
    val Gold = Color(0xFFC9A24B)
    val GoldLight = Color(0xFFE6CF92)
    val Ivory = Color(0xFFF6F1E7)
    val Sand = Color(0xFFEAE0CC)
    val Cream = Color(0xFFFFFDF8)
    val Ink = Color(0xFF14241F)
    val NightBg = Color(0xFF081512)
    val NightSurface = Color(0xFF0E211C)

    val headerBrush: Brush
        get() = Brush.verticalGradient(listOf(Emerald, EmeraldDeep))
}

private val LightColors = lightColorScheme(
    primary = Brand.Emerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EFE6),
    onPrimaryContainer = Brand.EmeraldDeep,
    secondary = Brand.Gold,
    onSecondary = Color(0xFF2A2000),
    secondaryContainer = Color(0xFFF3E7C6),
    onSecondaryContainer = Color(0xFF4A3A12),
    background = Brand.Ivory,
    onBackground = Brand.Ink,
    surface = Brand.Cream,
    onSurface = Brand.Ink,
    surfaceVariant = Brand.Sand,
    onSurfaceVariant = Color(0xFF505A52),
    outline = Color(0xFFB9A87E),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Brand.EmeraldLight,
    onPrimary = Color(0xFF00150F),
    primaryContainer = Brand.Emerald,
    onPrimaryContainer = Color.White,
    secondary = Brand.GoldLight,
    onSecondary = Color(0xFF2A2000),
    secondaryContainer = Color(0xFF5A4A1E),
    onSecondaryContainer = Brand.GoldLight,
    background = Brand.NightBg,
    onBackground = Color(0xFFE7EDE8),
    surface = Brand.NightSurface,
    onSurface = Color(0xFFE7EDE8),
    surfaceVariant = Color(0xFF24332D),
    onSurfaceVariant = Color(0xFFB9C6BE),
    outline = Color(0xFF6E7B72),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF2A0000)
)

@Composable
fun AzanTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
