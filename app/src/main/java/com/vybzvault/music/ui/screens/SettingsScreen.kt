package com.vybzvault.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vybzvault.music.AudioSettingsState
import com.vybzvault.music.ui.theme.AppThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    audioSettingsState: AudioSettingsState,
    selectedTheme: AppThemePreset,
    onSetTheme: (AppThemePreset) -> Unit,
    onOpenEqualizer: () -> Unit,
    onSetHighResEnabled: (Boolean) -> Unit,
    onSetShowAlbumArt: (Boolean) -> Unit = {},
    onSetCrossfadeSeconds: (Int) -> Unit = {},
    presets: List<Any> = emptyList(),
    selectedPresetId: String? = null,
    onSelectPreset: (String) -> Unit = {},
    onSaveCurrentAsPreset: () -> Unit = {},
    onResetToDefaults: () -> Unit = {},
    isUserSignedIn: Boolean = false
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // AUDIO

            item {
                SettingsGroup(title = "Audio") {

                    NavigationSettingRow(
                        title = "Equalizer",
                        subtitle = "10-band audio tuning",
                        onClick = onOpenEqualizer
                    )

                    DividerRow()

                    SwitchSettingRow(
                        title = "High Resolution Audio",
                        subtitle = "Enable enhanced playback quality",
                        checked = audioSettingsState.highResPlaybackEnabled,
                        onCheckedChange = onSetHighResEnabled
                    )
                }
            }

            // APPEARANCE

            item {
                SettingsGroup(title = "Appearance") {

                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ThemeSelector(
                        selectedTheme = selectedTheme,
                        onThemeSelected = onSetTheme
                    )
                }
            }

            // PLAYBACK

            item {
                SettingsGroup(title = "Playback") {

                    SwitchSettingRow(
                        title = "Show Album Art",
                        subtitle = "Display artwork across the app",
                        checked = audioSettingsState.showAlbumArt,
                        onCheckedChange = onSetShowAlbumArt
                    )

                    DividerRow()

                    CrossfadeSettingRow(
                        crossfadeSeconds = audioSettingsState.crossfadeSeconds,
                        onValueChange = onSetCrossfadeSeconds
                    )
                }
            }

            // ADVANCED

            item {
                SettingsGroup(title = "Advanced") {

                    TextButton(
                        onClick = onResetToDefaults,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Settings")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant//surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun NavigationSettingRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.Equalizer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CrossfadeSettingRow(
    crossfadeSeconds: Int,
    onValueChange: (Int) -> Unit
) {

    var sliderValue by remember {
        mutableStateOf(crossfadeSeconds.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Crossfade",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Smooth transition between songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AssistChip(
                onClick = {},
                label = {
                    Text("${sliderValue.toInt()} sec")
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it.toInt())
            },
            valueRange = 0f..12f
        )
    }
}

@Composable
private fun ThemeSelector(
    selectedTheme: AppThemePreset,
    onThemeSelected: (AppThemePreset) -> Unit
) {
    // Manual wrapping layout using FlowRow or a simple Column with multiple rows
    val themes = AppThemePreset.entries.toList()

    // Split themes into rows of 2-3 items each
    val rows = themes.chunked(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowThemes.forEach { theme ->
                    FilterChip(
                        selected = theme == selectedTheme,
                        onClick = { onThemeSelected(theme) },
                        label = { Text(theme.title) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Add empty spaces to maintain layout if needed
                repeat(3 - rowThemes.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DividerRow() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}