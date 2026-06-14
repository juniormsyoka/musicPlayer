package com.vybzvault.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════════
// BRAND COLORS — VybzVault Midnight Neon
// ═══════════════════════════════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════════════════════════════
// THEME VISUALS — Extra per-theme decorative tokens
// ═══════════════════════════════════════════════════════════════════════════════

@Stable
data class ThemeVisuals(
    val accent         : Color,
    val secondaryAccent: Color,
    val glassTint      : Color,
    val vignette       : Color,
    val glow           : Color,
    val isGlass        : Boolean = false
)

val LocalAppThemePreset = staticCompositionLocalOf { AppThemePreset.DEFAULT }

val LocalThemeVisuals = staticCompositionLocalOf {
    ThemeVisuals(
        accent          = VybzOrange,
        secondaryAccent = VaultCyan,
        glassTint       = Color(0x2200E5FF),
        vignette        = Color(0xAA05070A),
        glow            = Color(0x66FF7A00)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR SCHEMES
// ═══════════════════════════════════════════════════════════════════════════════

private val MidnightNeonColors = darkColorScheme(
    primary                = VybzOrange,
    onPrimary              = Color.White,
    primaryContainer       = VybzOrangeLight,
    onPrimaryContainer     = Color(0xFF1A0A00),

    secondary              = VaultCyan,
    onSecondary            = Color.Black,
    secondaryContainer     = VaultCyanDim,
    onSecondaryContainer   = Color(0xFF001A1F),

    background             = AbyssBlack,
    onBackground           = MidnightTextPrimary,

    surface                = MidnightBg,
    onSurface              = MidnightTextPrimary,
    surfaceVariant         = MidnightSurfaceVar,
    onSurfaceVariant       = MidnightTextSecondary,
    surfaceContainer       = MidnightSurface,
    surfaceContainerHigh   = MidnightSurfaceVar,
    surfaceContainerLowest = AbyssBlack,

    outline                = MidnightOutline,
    outlineVariant         = MidnightOutline.copy(alpha = 0.5f),

    scrim                  = MidnightScrim,

    error                  = NeonError,
    onError                = Color.White,
)

private val WaveformGreenColors = lightColorScheme(
    primary              = Color(0xFF1DB954),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFB7F5CE),
    onPrimaryContainer   = Color(0xFF00210E),

    secondary            = Color(0xFF191414),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD8D0CF),
    onSecondaryContainer = Color(0xFF0D0909),

    background           = Color(0xFFFFFFFF),
    onBackground         = Color(0xFF191414),

    surface              = Color(0xFFF4F4F4),
    onSurface            = Color(0xFF191414),
    surfaceVariant       = Color(0xFFE0E0E0),
    onSurfaceVariant     = Color(0xFF535353),
    surfaceContainer     = Color(0xFFEAEAEA),
    surfaceContainerHigh = Color(0xFFE0E0E0),

    outline              = Color(0xFFB3B3B3),
    outlineVariant       = Color(0xFFB3B3B3).copy(alpha = 0.5f),

    error                = Color(0xFFE22134),
    onError              = Color.White,
)

private val LofiBlushColors = lightColorScheme(
    primary              = Color(0xFF825500),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFFFDDB0),
    onPrimaryContainer   = Color(0xFF291800),

    secondary            = Color(0xFF9C4267),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFFFD9E4),
    onSecondaryContainer = Color(0xFF3E0021),

    background           = Color(0xFFF8F3EF),
    onBackground         = Color(0xFF201A18),  // fixed: was 0xFFD703FC (magenta — typo)

    surface              = Color(0xFFFFF8F4),
    onSurface            = Color(0xFF201A18),
    surfaceVariant       = Color(0xFFEBD2C7),
    onSurfaceVariant     = Color(0xFF53433F),
    surfaceContainer     = Color(0xFFF3E5DE),
    surfaceContainerHigh = Color(0xFFEBD2C7),

    outline              = Color(0xFF75615B),
    outlineVariant       = Color(0xFF75615B).copy(alpha = 0.5f),

    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
)

private val RetroAnalogColors = lightColorScheme(
    primary              = Color(0xFFBF360C),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFFFDBCF),
    onPrimaryContainer   = Color(0xFF3B0900),

    secondary            = Color(0xFF33691E),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFCBEFB0),
    onSecondaryContainer = Color(0xFF082100),

    background           = Color(0xFFF6EEDC),
    onBackground         = Color(0xFF2D2621),

    surface              = Color(0xFFFFF7EE),
    onSurface            = Color(0xFF2D2621),
    surfaceVariant       = Color(0xFFE4D6BE),
    onSurfaceVariant     = Color(0xFF4E4439),
    surfaceContainer     = Color(0xFFEEE3CE),
    surfaceContainerHigh = Color(0xFFE4D6BE),

    outline              = Color(0xFF6E6458),
    outlineVariant       = Color(0xFF6E6458).copy(alpha = 0.5f),

    error                = Color(0xFFB00020),
    onError              = Color.White,
)

// ═══════════════════════════════════════════════════════════════════════════════
// RESOLVERS
// ═══════════════════════════════════════════════════════════════════════════════

private fun resolveColorScheme(preset: AppThemePreset) = when (preset) {
    AppThemePreset.DEFAULT,
    AppThemePreset.MIDNIGHT_NEON  -> MidnightNeonColors
    AppThemePreset.WAVEFORM_GREEN -> WaveformGreenColors
    AppThemePreset.LO_FI_BLUSH    -> LofiBlushColors
    AppThemePreset.RETRO_ANALOG   -> RetroAnalogColors
}

private fun resolveThemeVisuals(preset: AppThemePreset) = when (preset) {
    AppThemePreset.DEFAULT,
    AppThemePreset.MIDNIGHT_NEON -> ThemeVisuals(
        accent          = VybzOrange,
        secondaryAccent = VaultCyan,
        glassTint       = Color(0x2200E5FF),
        vignette        = Color(0xAA05070A),
        glow            = Color(0x66FF7A00)
    )
    AppThemePreset.WAVEFORM_GREEN -> ThemeVisuals(
        accent          = Color(0xFF1DB954),
        secondaryAccent = Color(0xFF00E676),
        glassTint       = Color(0x331DB954),
        vignette        = Color(0x88060E0F),
        glow            = Color(0x551DB954)
    )
    AppThemePreset.LO_FI_BLUSH -> ThemeVisuals(
        accent          = Color(0xFF9C6ADE),
        secondaryAccent = Color(0xFFD58CB2),
        glassTint       = Color(0x44F8C8D8),
        vignette        = Color(0x33C99BB4),
        glow            = Color(0x669C6ADE)
    )
    AppThemePreset.RETRO_ANALOG -> ThemeVisuals(
        accent          = Color(0xFFCC5500),
        secondaryAccent = Color(0xFF4F772D),
        glassTint       = Color(0x44FFF6DB),
        vignette        = Color(0x66AF7A43),
        glow            = Color(0x66CC5500)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// ENTRY POINT
// ═══════════════════════════════════════════════════════════════════════════════


@Composable
fun MusicTheme(
    themePreset: AppThemePreset = AppThemePreset.DEFAULT,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppThemePreset provides themePreset,
        LocalThemeVisuals provides resolveThemeVisuals(themePreset)
    ) {
        MaterialTheme(
            colorScheme = resolveColorScheme(themePreset),
            typography = musicTypography(), //  This tells the whole app to use Poppins!
            content = content
        )
    }
}