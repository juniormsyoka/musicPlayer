package com.vybzvault.music.equalizer

import android.media.audiofx.Equalizer
import kotlin.math.abs

data class EqualizerCapabilities(
    val isSupported: Boolean,
    val bandRange: IntRange,
    val physicalBandCount: Int,
    val virtualBandToPhysicalBand: List<Short>
)

class EqualizerEngine {

    private var equalizer: Equalizer? = null
    private var mapping: List<Short> = List(EqualizerDefaults.BAND_FREQUENCIES_HZ.size) { 0 }
    private var range: IntRange = -1500..1500

    fun attach(audioSessionId: Int): EqualizerCapabilities {
        detach()
        if (audioSessionId <= 0) {
            return EqualizerCapabilities(false, range, 0, mapping)
        }

        return runCatching {
            val created = Equalizer(0, audioSessionId)
            equalizer = created

            val bandRange = created.bandLevelRange
            range = bandRange[0].toInt()..bandRange[1].toInt()

            val physicalBands = created.numberOfBands.toInt().coerceAtLeast(1)
            mapping = buildVirtualMapping(created)

            EqualizerCapabilities(
                isSupported = true,
                bandRange = range,
                physicalBandCount = physicalBands,
                virtualBandToPhysicalBand = mapping
            )
        }.getOrElse {
            equalizer = null
            EqualizerCapabilities(false, range, 0, mapping)
        }
    }

    fun detach() {
        equalizer?.release()
        equalizer = null
    }

    fun readVirtualBandLevels(): List<Int> {
        val eq = equalizer ?: return List(EqualizerDefaults.BAND_FREQUENCIES_HZ.size) { 0 }
        return mapping.map { mappedBand ->
            eq.getBandLevel(mappedBand).toInt().coerceIn(range.first, range.last)
        }
    }

    fun apply(enabled: Boolean, virtualBandLevelsMilliBel: List<Int>, preampDb: Float) {
        val eq = equalizer ?: return
        val normalizedLevels = normalizeLevels(virtualBandLevelsMilliBel)
        val preampMilliBel = EqualizerDefaults.dBToMilliBel(preampDb)

        val grouped = normalizedLevels.indices.groupBy { mapping[it].toInt() }
        for ((physicalBand, virtualIndexes) in grouped) {
            val averageVirtual = virtualIndexes
                .map { normalizedLevels[it] }
                .average()
                .toInt()
            val target = (averageVirtual + preampMilliBel).coerceIn(range.first, range.last)
            eq.setBandLevel(physicalBand.toShort(), target.toShort())
        }

        eq.enabled = enabled
    }

    private fun normalizeLevels(levels: List<Int>): List<Int> {
        return List(EqualizerDefaults.BAND_FREQUENCIES_HZ.size) { index ->
            val raw = levels.getOrNull(index) ?: 0
            raw.coerceIn(range.first, range.last)
        }
    }

    private fun buildVirtualMapping(equalizer: Equalizer): List<Short> {
        val physicalBands = (0 until equalizer.numberOfBands).map { it.toShort() }
        val centers = physicalBands.associateWith { band ->
            equalizer.getCenterFreq(band).toInt()
        }

        return EqualizerDefaults.BAND_FREQUENCIES_HZ.map { targetHz ->
            val targetMilliHz = targetHz * 1000
            physicalBands.minByOrNull { band ->
                abs((centers[band] ?: targetMilliHz) - targetMilliHz)
            } ?: 0
        }
    }
}
