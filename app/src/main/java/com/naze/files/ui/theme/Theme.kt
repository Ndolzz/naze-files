package com.naze.files.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NazeDarkColorScheme = darkColorScheme(
    primary = NazeBlue,
    onPrimary = Color.White,
    secondary = NazePurple,
    onSecondary = Color.White,
    background = NazeBackgroundDark,
    onBackground = NazeOnSurfaceDark,
    surface = NazeSurfaceDark,
    onSurface = NazeOnSurfaceDark,
    surfaceVariant = NazeSurfaceVariantDark,
    onSurfaceVariant = NazeOnSurfaceMutedDark,
    outline = NazeOutlineDark,
    error = NazeError,
)

private val NazeLightColorScheme = lightColorScheme(
    primary = NazeBlue,
    onPrimary = Color.White,
    secondary = NazePurple,
    onSecondary = Color.White,
    background = NazeBackgroundLight,
    onBackground = NazeOnSurfaceLight,
    surface = NazeSurfaceLight,
    onSurface = NazeOnSurfaceLight,
    surfaceVariant = NazeSurfaceVariantLight,
    onSurfaceVariant = NazeOnSurfaceMutedLight,
    outline = NazeOutlineLight,
    error = NazeError,
)

/**
 * App theme mode. [System] follows the OS setting; Naze Files itself
 * defaults new installs to [Dark] per the product spec, applied by whoever
 * reads the persisted Settings value (added in a later phase) and passes
 * it down to [NazeFilesTheme].
 */
enum class NazeThemeMode { Dark, Light, System }

@Composable
fun NazeFilesTheme(
    themeMode: NazeThemeMode = NazeThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        NazeThemeMode.Dark -> true
        NazeThemeMode.Light -> false
        NazeThemeMode.System -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDarkTheme) NazeDarkColorScheme else NazeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NazeTypography,
        content = content,
    )
}
