@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vybzvault.music.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vybzvault.music.EqualizerState
import com.vybzvault.music.equalizer.EqPreset
import com.vybzvault.music.equalizer.EqualizerDefaults
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    equalizerState: EqualizerState,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSetPreset: (EqPreset) -> Unit,
    onSetPreamp: (Float) -> Unit,
    onSetBandLevel: (index: Int, level: Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Enable Equalizer", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = when {
                                    equalizerState.error != null -> equalizerState.error
                                    equalizerState.audioSessionId > 0 -> "Attached to current playback session"
                                    else -> "Start playback to attach the equalizer"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = equalizerState.isEnabled,
                            onCheckedChange = onToggleEnabled,
                            enabled = equalizerState.audioSessionId > 0 || equalizerState.error == null
                        )
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Frequency Response", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        FrequencyResponseChart(equalizerState = equalizerState)
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Presets", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EqualizerDefaults.PRESET_ORDER.forEach { preset ->
                                FilterChip(
                                    selected = equalizerState.selectedPreset == preset,
                                    onClick = { onSetPreset(preset) },
                                    label = { Text(preset.label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val preampDisplay = String.format(Locale.US, "%.1f dB", equalizerState.preampDb)
                        Text("Preamp: $preampDisplay", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = equalizerState.preampDb,
                            onValueChange = onSetPreamp,
                            valueRange = EqualizerDefaults.PREAMP_MIN_DB..EqualizerDefaults.PREAMP_MAX_DB,
                            enabled = equalizerState.isEnabled
                        )
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("10-Band Graphic EQ", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Device bands: ${equalizerState.supportedBandCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        equalizerState.levels.forEachIndexed { index, level ->
                            val animatedLevel by animateFloatAsState(level.toFloat(), label = "eq_band_$index")
                            BandControlRow(
                                frequencyHz = equalizerState.bandFrequenciesHz.getOrElse(index) { 0 },
                                levelMilliBel = animatedLevel.roundToInt(),
                                range = equalizerState.range,
                                enabled = equalizerState.isEnabled,
                                onChange = { onSetBandLevel(index, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencyResponseChart(equalizerState: EqualizerState) {
    val min = equalizerState.range.first.toFloat().coerceAtMost(-1f)
    val max = equalizerState.range.second.toFloat().coerceAtLeast(1f)
    val span = (max - min).takeIf { it > 0f } ?: 1f
    val levels = equalizerState.levels

    val centerLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val curveColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        if (levels.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No equalizer bands available", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Attach to a playback session to render the curve",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / (levels.size - 1).coerceAtLeast(1)
                val centerY = size.height / 2f
                val path = Path()

                levels.forEachIndexed { index, value ->
                    val normalized = ((value - min) / span).coerceIn(0f, 1f)
                    val x = index * stepX
                    val y = size.height - (normalized * size.height)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawLine(
                    color = centerLineColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 2f
                )

                drawPath(path = path, color = curveColor, style = Stroke(width = 4f))
            }
        }
    }
}

@Composable
private fun BandControlRow(
    frequencyHz: Int,
    levelMilliBel: Int,
    range: Pair<Int, Int>,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    val dbText = String.format(Locale.US, "%.1f dB", EqualizerDefaults.milliBelToDb(levelMilliBel))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatFrequency(frequencyHz),
            modifier = Modifier.width(64.dp),
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = levelMilliBel.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.second.toFloat(),
            enabled = enabled
        )
        Text(
            text = dbText,
            modifier = Modifier.width(68.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


private fun formatFrequency(frequencyHz: Int): String {
    return if (frequencyHz >= 1000) {
        val inKhz = frequencyHz / 1000f
        if (inKhz % 1f == 0f) "${inKhz.toInt()}k" else String.format(Locale.US, "%.1fk", inKhz)
    } else {
        "$frequencyHz"
    }
}
