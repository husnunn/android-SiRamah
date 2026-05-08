package com.gridy.rohmahapp.pages.referensi

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RohmahGreenDark,
    onPrimary = RohmahCreamDark,
    secondary = RohmahBlueDark,
    onSecondary = RohmahCreamDark,
    tertiary = RohmahWarning,
    background = RohmahCreamDark,
    onBackground = RohmahSurfaceVariant,
    surface = RohmahSurfaceDark,
    onSurface = RohmahSurfaceVariant,
    surfaceVariant = RohmahSurfaceVariantDark,
    onSurfaceVariant = RohmahTextSubtleDark,
    outline = RohmahTextSubtleDark,
    error = RohmahError,
)

private val LightColorScheme = lightColorScheme(
    primary = RohmahGreen,
    onPrimary = RohmahSurface,
    secondary = RohmahBlue,
    onSecondary = RohmahSurface,
    tertiary = RohmahWarning,
    background = RohmahCream,
    onBackground = RohmahGreen,
    surface = RohmahSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF172019),
    surfaceVariant = RohmahSurfaceVariant,
    onSurfaceVariant = RohmahTextSubtle,
    outline = androidx.compose.ui.graphics.Color(0xFF8A968F),
    error = RohmahError,
)

private val AppTypography = Typography()

@Composable
fun AgonAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}