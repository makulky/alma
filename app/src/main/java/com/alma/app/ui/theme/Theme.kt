package com.alma.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A4A32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF2E1406),
    secondary = Color(0xFF6F5B4F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF8DFD1),
    onSecondaryContainer = Color(0xFF271911),
    tertiary = Color(0xFF3F6B57),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1EDD7),
    onTertiaryContainer = Color(0xFF002115),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A16),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A16),
    surfaceVariant = Color(0xFFF4DED4),
    onSurfaceVariant = Color(0xFF52443C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0B79D),
    onPrimary = Color(0xFF47210F),
    primaryContainer = Color(0xFF603620),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFDBC2B4),
    onSecondary = Color(0xFF3E2D23),
    secondaryContainer = Color(0xFF564338),
    onSecondaryContainer = Color(0xFFF8DFD1),
    tertiary = Color(0xFFA5D1BB),
    onTertiary = Color(0xFF0E3729),
    tertiaryContainer = Color(0xFF274E3F),
    onTertiaryContainer = Color(0xFFC1EDD7),
    background = Color(0xFF19120E),
    onBackground = Color(0xFFF0DFD8),
    surface = Color(0xFF19120E),
    onSurface = Color(0xFFF0DFD8),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B8),
)

@Composable
fun AlmaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
