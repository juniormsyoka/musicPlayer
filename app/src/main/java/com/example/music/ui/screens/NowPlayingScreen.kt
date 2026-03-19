@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.music.PlaybackState
import com.example.music.PlayerState
import com.example.music.RepeatMode
import com.example.music.Song
import com.example.music.ui.components.AlbumArt

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    volumePercent: Int,
    formatTime: (Long) -> String,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onSetRepeatMode: (RepeatMode) -> Unit,
    onSetVolumePercent: (Int) -> Unit,
    onUpdateQueue: (List<Song>) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    var showQueuePanel by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val activeSong = when (val state = playbackState.playerState) {
        is PlayerState.Ready -> state.song
        is PlayerState.Playing -> state.song
        is PlayerState.Paused -> state.song
        else -> null
    }
    val activePosition = when (val state = playbackState.playerState) {
        is PlayerState.Playing -> state.position
        is PlayerState.Paused -> state.position
        else -> 0L
    }
    val activeDuration = when (val state = playbackState.playerState) {
        is PlayerState.Playing -> state.duration
        is PlayerState.Paused -> state.duration
        else -> 0L
    }
    val playbackStatus = when (playbackState.playerState) {
        is PlayerState.Playing -> "Playing"
        is PlayerState.Paused -> "Paused"
        is PlayerState.Ready -> "Ready"
        is PlayerState.Preparing -> "Preparing"
        is PlayerState.Idle -> "Idle"
        is PlayerState.Error -> "Error"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.size(28.dp))
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { showInfoDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Track info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Player Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = playbackState.playerState) {
                is PlayerState.Idle, is PlayerState.Preparing -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                is PlayerState.Ready, is PlayerState.Playing, is PlayerState.Paused -> {
                    val song = when (state) {
                        is PlayerState.Ready -> state.song
                        is PlayerState.Playing -> state.song
                        is PlayerState.Paused -> state.song
                        else -> null
                    }
                    val position = (state as? PlayerState.Playing)?.position
                        ?: (state as? PlayerState.Paused)?.position ?: 0L
                    val duration = (state as? PlayerState.Playing)?.duration
                        ?: (state as? PlayerState.Paused)?.duration ?: 0L

                    if (song != null) {
                        NowPlayingContent(
                            song = song,
                            position = position,
                            duration = duration,
                            isPlaying = state is PlayerState.Playing,
                            repeatMode = playbackState.repeatMode,
                            shuffleEnabled = playbackState.shuffleEnabled,
                            formatTime = formatTime,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious,
                            onSeekTo = onSeekTo,
                            onToggleShuffle = onToggleShuffle,
                            onSetRepeatMode = onSetRepeatMode
                        )
                    }
                }

                is PlayerState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Playback Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Volume Control
        if (playbackState.playerState !is PlayerState.Idle &&
            playbackState.playerState !is PlayerState.Preparing
        )/* {
            VolumeControl(
                volumePercent = volumePercent,
                onVolumeChange = onSetVolumePercent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }*/

        // Queue Button
        Button(
            onClick = { showQueuePanel = !showQueuePanel },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.filledTonalButtonColors()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Queue (${playbackState.queue.size})")
        }
    }

    // Queue Panel
    if (showQueuePanel) {
        QueuePanel(
            queue = playbackState.queue,
            currentIndex = playbackState.currentIndex,
            onDismiss = { showQueuePanel = false },
            onSongSelected = { song ->
                onPlaySong(song, playbackState.queue)
            },
            onRemoveSongAt = { removeIndex ->
                if (removeIndex == playbackState.currentIndex) return@QueuePanel
                val next = playbackState.queue.toMutableList().apply {
                    if (removeIndex in indices) removeAt(removeIndex)
                }
                onUpdateQueue(next)
            },
            onClearUpcoming = {
                val current = playbackState.queue.getOrNull(playbackState.currentIndex)
                val next = if (current == null) emptyList() else listOf(current)
                onUpdateQueue(next)
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Now Playing Info") },
            text = {
                if (activeSong == null) {
                    Text("No song is currently loaded.")
                } else {
                    Text(
                        buildString {
                            appendLine("Title: ${activeSong.title}")
                            appendLine("Artist: ${activeSong.artist}")
                            appendLine("Album: ${activeSong.album}")
                            appendLine("Status: $playbackStatus")
                            appendLine("Position: ${formatTime(activePosition)}")
                            appendLine("Duration: ${formatTime(activeDuration)}")
                            appendLine("Queue: ${playbackState.currentIndex + 1}/${playbackState.queue.size}")
                            append("Repeat: ${playbackState.repeatMode} | Shuffle: ${if (playbackState.shuffleEnabled) "On" else "Off"}")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun NowPlayingContent(
    song: Song,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    formatTime: (Long) -> String,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onSetRepeatMode: (RepeatMode) -> Unit
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var draggedPosition by remember { mutableStateOf(position) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Album Art (Large)
        item {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 50) {
                                onSkipPrevious()
                            } else if (dragAmount < -50) {
                                onSkipNext()
                            }
                        }
                    }
            ) {
                AlbumArt(
                    uri = song.albumArtUri,
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Song Info
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Seekbar
        item {
            if (!isDraggingSlider) {
                draggedPosition = position
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (isDraggingSlider) draggedPosition.toFloat() else position.toFloat(),
                    onValueChange = { draggedPosition = it.toLong(); isDraggingSlider = true },
                    onValueChangeFinished = {
                        onSeekTo(draggedPosition)
                        isDraggingSlider = false
                    },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(draggedPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Control Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                // Shuffle Button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play/Pause Button
                Button(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = {
                        val nextMode = when (repeatMode) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                        onSetRepeatMode(nextMode)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.OFF -> Icons.Default.RepeatOne
                            RepeatMode.ALL -> Icons.Default.Repeat
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                        },
                        contentDescription = "Repeat mode",
                        tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VolumeControl(
    volumePercent: Int,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Slider(
                value = volumePercent.toFloat(),
                onValueChange = { onVolumeChange(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QueuePanel(
    queue: List<Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onRemoveSongAt: (Int) -> Unit,
    onClearUpcoming: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClearUpcoming, enabled = queue.isNotEmpty()) {
                    Text("Clear upcoming")
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Queue Items
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(queue, key = { index, song -> "queue-${song.id}-$index" }) { index, song ->
                val isCurrentSong = index == currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSongSelected(song) }
                        .background(
                            if (isCurrentSong) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isCurrentSong) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isCurrentSong) {
                        IconButton(onClick = { onRemoveSongAt(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove from queue",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}











