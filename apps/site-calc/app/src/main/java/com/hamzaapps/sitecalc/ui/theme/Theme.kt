package com.hamzaapps.sitecalc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand: safety orange with steel blue-grey.
private val LightColors = lightColorScheme(
    primary = Color(0xFFB85000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF465A6B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E4F7),
    onSecondaryContainer = Color(0xFF021D2E),
    tertiary = Color(0xFF5B6300),
    tertiaryContainer = Color(0xFFDFEA7F),
    onTertiaryContainer = Color(0xFF1A1E00),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
    surfaceVariant = Color(0xFFF4DED3),
    onSurfaceVariant = Color(0xFF52443C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68C),
    onPrimary = Color(0xFF532200),
    primaryContainer = Color(0xFF8C3C00),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFB4C8DB),
    onSecondary = Color(0xFF1E3344),
    secondaryContainer = Color(0xFF34495B),
    onSecondaryContainer = Color(0xFFD0E4F7),
    tertiary = Color(0xFFC3CE66),
    tertiaryContainer = Color(0xFF444B00),
    onTertiaryContainer = Color(0xFFDFEA7F),
    background = Color(0xFF1A120D),
    surface = Color(0xFF1A120D),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B8),
)

@Composable
fun SiteCalcTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
