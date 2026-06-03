package com.ulm.azan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenPrimary = Color(0xFF2E7D32)
private val GreenDark = Color(0xFF1B5E20)
private val GreenContainer = Color(0xFFB7E1B9)
private val Gold = Color(0xFFB58A2E)

private val AzanColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenDark,
    secondary = Gold,
    onSecondary = Color.White,
    background = Color(0xFFF7FAF6),
    onBackground = Color(0xFF1A1C19),
    surface = Color.White,
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE3E8E0),
    onSurfaceVariant = Color(0xFF424940),
)

@Composable
fun AzanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AzanColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
