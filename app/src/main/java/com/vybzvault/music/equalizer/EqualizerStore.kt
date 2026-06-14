package com.vybzvault.music.equalizer

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

data class PersistedEqualizerConfig(
    val enabled: Boolean,
    val selectedPreset: EqPreset,
    val preampDb: Float,
    val bandLevelsMilliBel: List<Int>
)

class EqualizerStore(context: Context) {

    private val prefs = context.getSharedPreferences("equalizer_store", Context.MODE_PRIVATE)

    fun load(): PersistedEqualizerConfig {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val preset = EqPreset.fromName(prefs.getString(KEY_PRESET, EqPreset.CUSTOM.name))
        val preampDb = prefs.getFloat(KEY_PREAMP_DB, 0f)
        val bandLevels = decodeBandLevels(
            prefs.getString(KEY_BAND_LEVELS, null)
        ) ?: List(EqualizerDefaults.BAND_FREQUENCIES_HZ.size) { 0 }

        return PersistedEqualizerConfig(
            enabled = enabled,
            selectedPreset = preset,
            preampDb = preampDb,
            bandLevelsMilliBel = bandLevels
        )
    }

    fun save(config: PersistedEqualizerConfig) {
        prefs.edit {
            putBoolean(KEY_ENABLED, config.enabled)
            putString(KEY_PRESET, config.selectedPreset.name)
            putFloat(KEY_PREAMP_DB, config.preampDb)
            putString(KEY_BAND_LEVELS, encodeBandLevels(config.bandLevelsMilliBel))
        }
    }

    private fun encodeBandLevels(levels: List<Int>): String {
        return JSONArray().apply { levels.forEach(::put) }.toString()
    }

    private fun decodeBandLevels(raw: String?): List<Int>? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getInt(index))
                }
            }
        }.getOrNull()
    }

    companion object {
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_PRESET = "eq_selected_preset"
        private const val KEY_PREAMP_DB = "eq_preamp_db"
        private const val KEY_BAND_LEVELS = "eq_band_levels"
    }
}


