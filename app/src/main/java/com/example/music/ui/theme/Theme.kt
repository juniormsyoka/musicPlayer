package com.example.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Spotify-inspired palette ──────────────────────────────────────────────────
val SpotifyGreenDark    = Color(0xFF158A3E)
val SpotifyGreenDim     = Color(0x261DB954)

val Surface0            = Color(0xFF121212)   // page background
val Surface1            = Color(0xFF181818)   // card / nav bar
val Surface2            = Color(0xFF242424)   // elevated card
val Surface3            = Color(0xFF2A2A2A)   // hover / selected
val Surface4            = Color(0xFF333333)   // input / chip

val TextPrimary         = Color(0xFFFFFFFF)
val TextSecondary       = Color(0xFFB3B3B3)

val ErrorRed            = Color(0xFFE74C3C)

// ── Dark scheme (primary) ─────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary              = SpotifyGreen,
    onPrimary            = Color.Black,
    primaryContainer     = SpotifyGreenDim,
    onPrimaryContainer   = SpotifyGreen,
    secondary            = TextSecondary,
    onSecondary          = Color.Black,
    secondaryContainer   = Surface3,
    onSecondaryContainer = TextPrimary,
    tertiary             = SpotifyGreen,
    onTertiary           = Color.Black,
    background           = Surface0,
    onBackground         = TextPrimary,
    surface              = Surface1,
    onSurface            = TextPrimary,
    surfaceVariant       = Surface2,
    onSurfaceVariant     = TextSecondary,
    outline              = Surface4,
    error                = ErrorRed,
    onError              = Color.White,
)

// ── Light scheme (fallback) ───────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary              = SpotifyGreen,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFE8F8EE),
    onPrimaryContainer   = SpotifyGreenDark,
    secondary            = Color(0xFF4A4A4A),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFF0F0F0),
    onSecondaryContainer = Color(0xFF121212),
    background           = Color(0xFFF8F8F8),
    onBackground         = Color(0xFF121212),
    surface              = Color.White,
    onSurface            = Color(0xFF121212),
    surfaceVariant       = Color(0xFFF0F0F0),
    onSurfaceVariant     = Color(0xFF4A4A4A),
    outline              = Color(0xFFDDDDDD),
    error                = ErrorRed,
    onError              = Color.White,
)

@Composable
fun MusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}