package com.vybzvault.music

import android.content.Context
import androidx.core.content.edit

class AudioSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("audio_settings_store", Context.MODE_PRIVATE)

    fun load(): AudioSettingsState {
        return AudioSettingsState(
            highResPlaybackEnabled = prefs.getBoolean(KEY_HIGH_RES, false),
            bitPerfectEnabled = prefs.getBoolean(KEY_BIT_PERFECT, false),
            sampleRatePreference = SampleRatePreference.entries.firstOrNull {
                it.name == prefs.getString(KEY_SAMPLE_RATE, SampleRatePreference.AUTO.name)
            } ?: SampleRatePreference.AUTO,
            reverbPreset = ReverbPreset.entries.firstOrNull {
                it.name == prefs.getString(KEY_REVERB, ReverbPreset.OFF.name)
            } ?: ReverbPreset.OFF,
            crossfadeSeconds = AudioSettingsState.clampCrossfadeSeconds(
                prefs.getInt(KEY_CROSSFADE_SECONDS, AudioSettingsState.MIN_CROSSFADE_SECONDS)
            ),
            volumeNormalizationEnabled = prefs.getBoolean(KEY_VOLUME_NORMALIZATION, false),
            showAlbumArt = prefs.getBoolean(KEY_SHOW_ALBUM_ART, true)
        )
    }

    fun save(value: AudioSettingsState) {
        prefs.edit {
            putBoolean(KEY_HIGH_RES, value.highResPlaybackEnabled)
            putBoolean(KEY_BIT_PERFECT, value.bitPerfectEnabled)
            putString(KEY_SAMPLE_RATE, value.sampleRatePreference.name)
            putString(KEY_REVERB, value.reverbPreset.name)
            putInt(KEY_CROSSFADE_SECONDS, value.crossfadeSeconds)
            putBoolean(KEY_SHOW_ALBUM_ART, value.showAlbumArt)
            putBoolean(KEY_VOLUME_NORMALIZATION, value.volumeNormalizationEnabled)
        }
    }

    companion object {
        private const val KEY_HIGH_RES = "high_res"
        private const val KEY_BIT_PERFECT = "bit_perfect"
        private const val KEY_SAMPLE_RATE = "sample_rate"
        private const val KEY_REVERB = "reverb"
        private const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
        private const val KEY_SHOW_ALBUM_ART = "show_album_art"
        private const val KEY_VOLUME_NORMALIZATION = "volume_normalization"
    }
}
