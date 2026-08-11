package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FrostedGlassColorScheme = lightColorScheme(
    primary = GamingPrimaryGold,
    onPrimary = Color.White,
    secondary = GamingAccentCyan,
    onSecondary = Color.White,
    tertiary = GamingAccentPink,
    background = GamingDarkBackground,
    onBackground = GamingTextPrimary,
    surface = GamingSurface,
    onSurface = GamingTextPrimary,
    surfaceVariant = GamingCardSurface,
    onSurfaceVariant = GamingTextSecondary,
    error = GamingErrorRed,
    onError = Color.White
)

@Composable
fun TournamentGamingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FrostedGlassColorScheme,
        typography = Typography,
        content = content
    )
}


