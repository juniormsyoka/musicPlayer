package com.vybzvault.music.equalizer

enum class EqPreset(val label: String) {
    ROCK("Rock"),
    POP("Pop"),
    JAZZ("Jazz"),
    CLASSICAL("Classical"),
    BASS_BOOST("Bass Boost"),
    CUSTOM("Custom");

    companion object {
        fun fromName(value: String?): EqPreset {
            return entries.firstOrNull { it.name == value } ?: CUSTOM
        }
    }
}

object EqualizerDefaults {
    val BAND_FREQUENCIES_HZ = listOf(32, 64, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)

    const val PREAMP_MIN_DB = -12f
    const val PREAMP_MAX_DB = 12f

    val PRESET_ORDER = listOf(
        EqPreset.ROCK,
        EqPreset.POP,
        EqPreset.JAZZ,
        EqPreset.CLASSICAL,
        EqPreset.BASS_BOOST,
        EqPreset.CUSTOM
    )

    private val PRESET_DB = mapOf(
        EqPreset.ROCK to listOf(4f, 3f, 2f, 1f, -1f, -1f, 1f, 2f, 3f, 4f),
        EqPreset.POP to listOf(-1f, 1f, 3f, 4f, 3f, 1f, -1f, -2f, -1f, 0f),
        EqPreset.JAZZ to listOf(2f, 1f, 0f, 1f, 2f, 2f, 1f, 0f, 1f, 2f),
        EqPreset.CLASSICAL to listOf(0f, 0f, 0f, 1f, 2f, 2f, 1f, 1f, 0f, 0f),
        EqPreset.BASS_BOOST to listOf(6f, 5f, 4f, 2f, 0f, -1f, -2f, -3f, -3f, -3f)
    )

    fun presetLevelsMilliBel(preset: EqPreset, range: IntRange): List<Int> {
        val db = PRESET_DB[preset] ?: return List(BAND_FREQUENCIES_HZ.size) { 0 }
        return db.map { dBToMilliBel(it).coerceIn(range.first, range.last) }
    }

    fun dBToMilliBel(value: Float): Int = (value * 100f).toInt()

    fun milliBelToDb(value: Int): Float = value / 100f
}

