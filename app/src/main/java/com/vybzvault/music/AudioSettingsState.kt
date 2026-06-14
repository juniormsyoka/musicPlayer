package com.vybzvault.music

import com.vybzvault.music.ui.theme.AppThemePreset

enum class SampleRatePreference {
    AUTO,
    HZ_44_1,
    HZ_48,
    HZ_96,
    HZ_192
}

enum class ReverbPreset {
    OFF,
    ROOM,
    HALL,
    PLATE
}

data class AudioSettingsState(
    val highResPlaybackEnabled: Boolean = false,
    val bitPerfectEnabled: Boolean = false,
    val sampleRatePreference: SampleRatePreference = SampleRatePreference.AUTO,
    val reverbPreset: ReverbPreset = ReverbPreset.OFF,
    val crossfadeSeconds: Int = 0,
    val volumeNormalizationEnabled: Boolean = false,
    val showAlbumArt: Boolean = true
) {
    companion object {
        const val MIN_CROSSFADE_SECONDS = 0
        const val MAX_CROSSFADE_SECONDS = 12

        fun clampCrossfadeSeconds(value: Int): Int {
            return value.coerceIn(MIN_CROSSFADE_SECONDS, MAX_CROSSFADE_SECONDS)
        }
    }
}

data class SettingsPreset(
    val id: String,
    val title: String,
    val description: String,
    val audioSettings: AudioSettingsState,
    val themePreset: AppThemePreset? = null,
    val isTemplate: Boolean = true
)

fun builtInSettingsPresets(): List<SettingsPreset> = listOf(
    SettingsPreset(
        id = "car",
        title = "Car",
        description = "Louder with more bass.",
        audioSettings = AudioSettingsState(
            reverbPreset = ReverbPreset.ROOM,
            crossfadeSeconds = 2,
            volumeNormalizationEnabled = true
        ),
        themePreset = AppThemePreset.WAVEFORM_GREEN
    ),
    SettingsPreset(
        id = "late-night",
        title = "Late night",
        description = "Quieter playback with no bass emphasis.",
        audioSettings = AudioSettingsState(
            crossfadeSeconds = 6,
            volumeNormalizationEnabled = true
        ),
        themePreset = AppThemePreset.LO_FI_BLUSH
    ),
    SettingsPreset(
        id = "voice-focus",
        title = "Voice Focus",
        description = "Heavy bass-cut and sharp boost between 2kHz and 4kHz.",
        audioSettings = AudioSettingsState(
            sampleRatePreference = SampleRatePreference.HZ_48,
            crossfadeSeconds = 1,
            volumeNormalizationEnabled = true
        ),
        themePreset = AppThemePreset.RETRO_ANALOG
    ),
    SettingsPreset(
        id = "open-road",
        title = "Open Road",
        description = "Boosts high-mids and treble.",
        audioSettings = AudioSettingsState(
            highResPlaybackEnabled = true,
            sampleRatePreference = SampleRatePreference.HZ_96,
            reverbPreset = ReverbPreset.HALL,
            crossfadeSeconds = 3
        ),
        themePreset = AppThemePreset.WAVEFORM_GREEN
    ),
    SettingsPreset(
        id = "gym-workout",
        title = "Gym workout",
        description = "Aggressive bass, compressed dynamics, and max volume limit.",
        audioSettings = AudioSettingsState(
            highResPlaybackEnabled = true,
            sampleRatePreference = SampleRatePreference.HZ_96,
            reverbPreset = ReverbPreset.PLATE,
            crossfadeSeconds = 8,
            volumeNormalizationEnabled = true
        ),
        themePreset = AppThemePreset.WAVEFORM_GREEN
    ),
    SettingsPreset(
        id = "beach-pool",
        title = "Beach/pool",
        description = "Weather-resistant mode for strong background noise.",
        audioSettings = AudioSettingsState(
            sampleRatePreference = SampleRatePreference.HZ_44_1,
            reverbPreset = ReverbPreset.OFF,
            crossfadeSeconds = 0,
            volumeNormalizationEnabled = true
        ),
        themePreset = AppThemePreset.RETRO_ANALOG
    )
)
