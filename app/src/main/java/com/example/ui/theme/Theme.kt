package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val TerminalDarkColorScheme = darkColorScheme(
    primary = TerminalWhite,
    onPrimary = TerminalBlack,
    primaryContainer = TerminalSurfaceVariant,
    onPrimaryContainer = TerminalWhite,
    secondary = TerminalTextSecondary,
    onSecondary = TerminalBlack,
    secondaryContainer = TerminalSurface,
    onSecondaryContainer = TerminalTextPrimary,
    tertiary = StatusGreen,
    onTertiary = TerminalBlack,
    tertiaryContainer = Color(0xFF142918),
    onTertiaryContainer = StatusGreen,
    background = TerminalBackground,
    onBackground = TerminalTextPrimary,
    surface = TerminalSurface,
    onSurface = TerminalTextPrimary,
    surfaceVariant = TerminalSurfaceVariant,
    onSurfaceVariant = TerminalTextSecondary,
    outline = TerminalBorder,
    outlineVariant = TerminalBorderLight,
    error = StatusRed,
    onError = TerminalWhite
)

private val TerminalLightColorScheme = lightColorScheme(
    primary = TerminalBlack,
    onPrimary = TerminalWhite,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = TerminalBlack,
    secondary = Color(0xFF404040),
    onSecondary = TerminalWhite,
    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = TerminalBlack,
    tertiary = StatusGreen,
    onTertiary = TerminalWhite,
    background = Color(0xFFFAFAFA),
    onBackground = TerminalBlack,
    surface = TerminalWhite,
    onSurface = TerminalBlack,
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0xFF525252),
    outline = Color(0xFFD4D4D4),
    error = StatusRed,
    onError = TerminalWhite
)

@Composable
fun MobileCroakerTheme(
    darkTheme: Boolean = true, // Default to terminal dark
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TerminalDarkColorScheme else TerminalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun OpenClawTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MobileCroakerTheme(darkTheme = darkTheme, content = content)
