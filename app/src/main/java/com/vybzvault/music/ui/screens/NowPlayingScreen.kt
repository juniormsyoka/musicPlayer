@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.vybzvault.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vybzvault.music.PlaybackState
import com.vybzvault.music.PlayerState
import com.vybzvault.music.RepeatMode
import com.vybzvault.music.Song
import com.vybzvault.music.ui.components.AlbumArt
import com.vybzvault.music.ui.theme.LocalThemeVisuals

// ─── Root Screen ──────────────────────────────────────────────────────────────

@Composable
fun NowPlayingScreen(
    playbackState     : PlaybackState,
    showAlbumArt      : Boolean,
    formatTime        : (Long) -> String,
    onTogglePlayPause : () -> Unit,
    onSkipNext        : () -> Unit,
    onSkipPrevious    : () -> Unit,
    onSeekTo          : (Long) -> Unit,
    onToggleShuffle   : () -> Unit,
    onSetRepeatMode   : (RepeatMode) -> Unit,
    onUpdateQueue     : (List<Song>) -> Unit,
    onPlaySong        : (Song, List<Song>) -> Unit,
    onPlayNext        : (Song) -> Unit,
    onOpenEqualizer   : () -> Unit = {}
) {
    var showQueue      by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val activeSong = when (val s = playbackState.playerState) {
        is PlayerState.Ready   -> s.song
        is PlayerState.Playing -> s.song
        is PlayerState.Paused  -> s.song
        else                   -> null
    }
    val activePosition = when (val s = playbackState.playerState) {
        is PlayerState.Playing -> s.position
        is PlayerState.Paused  -> s.position
        else                   -> 0L
    }
    val activeDuration = when (val s = playbackState.playerState) {
        is PlayerState.Playing -> s.duration
        is PlayerState.Paused  -> s.duration
        else                   -> 0L
    }
    val isPlaying = playbackState.playerState is PlayerState.Playing

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-bleed blurred atmosphere ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (activeSong != null && showAlbumArt) {
                AlbumArt(
                    uri         = activeSong.albumArtUri,
                    modifier    = Modifier
                        .fillMaxSize()
                        .blur(72.dp)
                        .graphicsLayer { alpha = 0.28f },
                    showArtwork = true
                )
            }
            val themeVisuals = LocalThemeVisuals.current
            // Graduated scrim — denser at top (controls legibility) and bottom (seekbar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to themeVisuals.glassTint.copy(alpha = 0.62f),
                            0.45f to themeVisuals.glassTint.copy(alpha = 0.18f),
                            1.00f to themeVisuals.vignette.copy(alpha = 0.82f)
                        )
                    )
            )
        }

        // ── Main content column ───────────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NowPlayingTopBar(
                onOpenEqualizer = onOpenEqualizer,
                onShowInfo      = { showInfoDialog = true }
            )

            // Player body
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = playbackState.playerState) {
                    is PlayerState.Idle      -> NowPlayingEmptyState()
                    is PlayerState.Preparing -> {
                        CircularProgressIndicator(
                            color       = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier    = Modifier.size(52.dp)
                        )
                    }
                    is PlayerState.Error     -> NowPlayingErrorState(message = state.message)
                    else -> {
                        val song = activeSong ?: return@Box
                        NowPlayingContent(
                            song              = song,
                            position          = activePosition,
                            duration          = activeDuration,
                            isPlaying         = isPlaying,
                            showAlbumArt      = showAlbumArt,
                            repeatMode        = playbackState.repeatMode,
                            shuffleEnabled    = playbackState.shuffleEnabled,
                            formatTime        = formatTime,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext        = onSkipNext,
                            onSkipPrevious    = onSkipPrevious,
                            onSeekTo          = onSeekTo,
                            onToggleShuffle   = onToggleShuffle,
                            onSetRepeatMode   = onSetRepeatMode
                        )
                    }
                }
            }

            QueueButtonStrip(
                queueSize   = playbackState.queue.size,
                onShowQueue = { showQueue = true }
            )
        }

        // ── Queue sheet overlay ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showQueue,
            enter   = fadeIn(tween(220)) + slideInVertically(tween(280)) { it },
            exit    = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.54f))
                    .clickable(
                        indication    = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showQueue = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.74f)
                        .align(Alignment.BottomCenter)
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { /* consume */ },
                    shape         = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color         = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation= 3.dp
                ) {
                    // NowPlayingScreen — fix the call site
                    QueuePanel(
                        queue            = playbackState.queue,          // ← full queue, not dropped
                        currentIndex     = playbackState.currentIndex,
                        onDismiss        = { showQueue = false },
                        onSongSelected   = { song ->
                            onPlaySong(song, playbackState.queue)
                            showQueue = false
                        },
                        onRemoveSongAt   = { idx ->
                            if (idx == playbackState.currentIndex) return@QueuePanel
                            onUpdateQueue(playbackState.queue.toMutableList().apply {
                                if (idx in indices) removeAt(idx)
                            })
                        },
                        onPlayNextAt     = { idx ->
                            playbackState.queue.getOrNull(idx)?.let(onPlayNext)  // ← idx is now a real index
                        },
                        onClearUpcoming  = {
                            // Keep everything up to and including current song
                            onUpdateQueue(
                                playbackState.queue.take(playbackState.currentIndex + 1)
                            )
                        }
                    )
                }
            }
        }
    }

    // ── Info dialog ────────────────────────────────────────────────────────────
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    "Track info",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (activeSong == null) {
                    Text(
                        "No song loaded.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow("Title",    activeSong.title)
                        InfoRow("Artist",   activeSong.artist)
                        InfoRow("Album",    activeSong.album)
                        InfoRow("Position", formatTime(activePosition))
                        InfoRow("Duration", formatTime(activeDuration))
                        InfoRow("Queue",
                            "${playbackState.currentIndex + 1} of ${playbackState.queue.size}")
                        InfoRow("Repeat",
                            playbackState.repeatMode.name.lowercase()
                                .replaceFirstChar { it.uppercase() })
                        InfoRow("Shuffle",  if (playbackState.shuffleEnabled) "On" else "Off")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(
                        "Done",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun NowPlayingTopBar(
    onOpenEqualizer: () -> Unit,
    onShowInfo     : () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Eyebrow label using primaryContainer surface
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
        ) {
            Text(
                text          = "Now playing",
                style         = MaterialTheme.typography.labelMedium,
                fontWeight    = FontWeight.Bold,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp,
                modifier      = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }

        Row {
            IconButton(onClick = onOpenEqualizer) {
                Icon(
                    imageVector       = Icons.Default.Equalizer,
                    contentDescription= "Open equalizer",
                    tint              = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier          = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onShowInfo) {
                Icon(
                    imageVector       = Icons.Default.Info,
                    contentDescription= "Track info",
                    tint              = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier          = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ─── Now Playing Content ──────────────────────────────────────────────────────

@Composable
private fun NowPlayingContent(
    song             : Song,
    position         : Long,
    duration         : Long,
    isPlaying        : Boolean,
    showAlbumArt     : Boolean,
    repeatMode       : RepeatMode,
    shuffleEnabled   : Boolean,
    formatTime       : (Long) -> String,
    onTogglePlayPause: () -> Unit,
    onSkipNext       : () -> Unit,
    onSkipPrevious   : () -> Unit,
    onSeekTo         : (Long) -> Unit,
    onToggleShuffle  : () -> Unit,
    onSetRepeatMode  : (RepeatMode) -> Unit
) {
    var isDragging      by remember { mutableStateOf(false) }
    var draggedPosition by remember { mutableLongStateOf(position) }

    // ── Single shared infinite transition (no duplicates) ─────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "playerAnim")

    // Vinyl rotation — only spin when playing
    val vinylAngle by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            tween(8_000, easing = LinearEasing),
            androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "vinylAngle"
    )

    // Subtle breathing scale when playing — calm, not distracting
    val breathScale by infiniteTransition.animateFloat(
        initialValue  = 1.00f,
        targetValue   = 1.025f,
        animationSpec = infiniteRepeatable(
            tween(1_800, easing = FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // Equalizer bars
    val eqBar1 by infiniteTransition.animateFloat(
        initialValue  = 6f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "eq1"
    )
    val eqBar2 by infiniteTransition.animateFloat(
        initialValue  = 16f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(580, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "eq2"
    )
    val eqBar3 by infiniteTransition.animateFloat(
        initialValue  = 9f, targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "eq3"
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),


        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Vinyl / Album Art ─────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .graphicsLayer {
                    scaleX = if (isPlaying) breathScale else 1f
                    scaleY = if (isPlaying) breathScale else 1f
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, delta ->
                        if (delta > 60f)       onSkipPrevious()
                        else if (delta < -60f) onSkipNext()
                    }
                }
        ) {
            // Soft radial glow — uses M3 primaryContainer, adapts to any theme
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                                Color.Transparent
                            )
                        )
                    )
                    .blur(18.dp)
            )

            // Vinyl disc
            Box(
                modifier         = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .rotate(if (isPlaying) vinylAngle else 0f)
                    .shadow(10.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves — subtle, purely decorative rings
                val grooveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                (1..6).forEach { ring ->
                    Box(
                        modifier = Modifier
                            .size((272 - ring * 26).dp)
                            .clip(CircleShape)
                            .border(1.dp, grooveColor, CircleShape)
                    )
                }

                // Album art centred on disc
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .shadow(12.dp, CircleShape)
                ) {
                    AlbumArt(
                        uri         = song.albumArtUri,
                        modifier    = Modifier.fillMaxSize(),
                        showArtwork = showAlbumArt
                    )
                }

                // Centre spindle — uses primary for brand accent
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(
                            1.5.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            CircleShape
                        )
                )
            }


        }

        Spacer(Modifier.height(36.dp))

        // ── Song info ─────────────────────────────────────────────────────────
        // ── Song info with fixed height container ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp), // Fixed height accommodates 2-line title + artist + spacing
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text      = song.title,
                    style     = MaterialTheme.typography.headlineSmall,
                    fontWeight= FontWeight.Bold,
                    color     = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis,
                    modifier  = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = song.artist,
                    style    = MaterialTheme.typography.bodyLarge,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign= TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Seekbar ───────────────────────────────────────────────────────────
        if (!isDragging) draggedPosition = position

        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value               = draggedPosition.toFloat(),
                onValueChange       = { draggedPosition = it.toLong(); isDragging = true },
                onValueChangeFinished = { onSeekTo(draggedPosition); isDragging = false },
                valueRange          = 0f..duration.coerceAtLeast(1L).toFloat(),
                modifier            = Modifier.fillMaxWidth(),
                colors              = SliderDefaults.colors(
                    thumbColor         = MaterialTheme.colorScheme.primary,
                    activeTrackColor   = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.onPrimary.copy(0.35f), CircleShape)
                    )
                }
            )
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = formatTime(draggedPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = formatTime(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Transport controls ────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Shuffle
            ToggleIconButton(
                icon               = Icons.Default.Shuffle,
                active             = shuffleEnabled,
                size               = 40.dp,
                onClick            = onToggleShuffle,
                contentDescription = "Toggle shuffle"
            )

            // Previous
            TransportButton(
                icon               = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                size               = 54.dp,
                onClick            = onSkipPrevious
            )

            // Play / Pause — M3 FilledIconButton sizing convention
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .shadow(16.dp, CircleShape)
                    .clickable(onClick = onTogglePlayPause)
            ) {
                // Inner highlight ring
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                            CircleShape
                        )
                )
                AnimatedContent(
                    targetState  = isPlaying,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                    label        = "playPauseIcon"
                ) { playing ->
                    Icon(
                        imageVector       = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription= if (playing) "Pause" else "Play",
                        tint              = MaterialTheme.colorScheme.onPrimary,
                        modifier          = Modifier.size(36.dp)
                    )
                }
            }

            // Next
            TransportButton(
                icon               = Icons.Default.SkipNext,
                contentDescription = "Next",
                size               = 54.dp,
                onClick            = onSkipNext
            )

            // Repeat
            val repeatIcon = when (repeatMode) {
                RepeatMode.ONE -> Icons.Default.RepeatOne
                else           -> Icons.Default.Repeat
            }
            ToggleIconButton(
                icon   = repeatIcon,
                active = repeatMode != RepeatMode.OFF,
                size   = 40.dp,
                onClick = {
                    onSetRepeatMode(
                        when (repeatMode) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                    )
                },
                contentDescription = "Toggle repeat"
            )
        }

        Spacer(Modifier.height(10.dp))
    }
}

// ─── Queue Button Strip ───────────────────────────────────────────────────────

@Composable
private fun QueueButtonStrip(queueSize: Int, onShowQueue: () -> Unit) {
    Box(
        modifier        = Modifier
            .fillMaxWidth()

            .padding(horizontal = 24.dp, vertical = 10.dp),

        contentAlignment= Alignment.Center
    ) {
        Surface(
            onClick       = onShowQueue,
            shape         = RoundedCornerShape(30.dp),
            color         = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            border        = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            tonalElevation= 1.dp
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector       = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription= null,
                    tint              = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier          = Modifier.size(18.dp)
                )
                Text(

                    text          = "Queue  ·  $queueSize upcoming",
                    style         = MaterialTheme.typography.labelLarge,
                    fontWeight    = FontWeight.SemiBold,
                    color         = MaterialTheme.colorScheme.onSecondaryContainer,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

// ─── Queue Panel ──────────────────────────────────────────────────────────────

@Composable
private fun QueuePanel(
    queue          : List<Song>,
    currentIndex   : Int,
    onDismiss      : () -> Unit,
    onSongSelected : (Song) -> Unit,
    onRemoveSongAt : (Int) -> Unit,
    onPlayNextAt   : (Int) -> Unit,
    onClearUpcoming: () -> Unit
) {
    val upcoming = queue.drop(currentIndex + 1)
    Column(modifier = Modifier.fillMaxSize()) {

        // Drag handle
        Box(
            modifier         = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp, bottom = 8.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Header row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text  = "Upcoming",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "${upcoming.size} upcoming songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (queue.size > 1) {
                    TextButton(onClick = onClearUpcoming) {
                        Text(
                            "Clear upcoming",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(
            color     = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            itemsIndexed(upcoming, key = { i, s -> "q-${s.id}-$i" }) { localIndex, song ->
                val realIndex = currentIndex + 1 + localIndex
                QueueSongRow(
                    song       = song,
                    index      = localIndex,          // display number (1-based label)
                    isCurrent  = false,               // upcoming list never has current
                    onSelect   = { onSongSelected(song) },
                    onPlayNext = { onPlayNextAt(realIndex) },     // ← real index
                    onRemove   = { onRemoveSongAt(realIndex) }    // ← real index
                )
            }
        }
    }
}

@Composable
private fun QueueSongRow(
    song     : Song,
    index    : Int,
    isCurrent: Boolean,
    onSelect : () -> Unit,
    onPlayNext: () -> Unit,
    onRemove : () -> Unit
) {
    // Shared infinite transition for eq bars in row
    val infiniteTransition = rememberInfiniteTransition(label = "rowEq")
    val eqB1 by infiniteTransition.animateFloat(
        initialValue  = 5f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(430, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "rEq1"
    )
    val eqB2 by infiniteTransition.animateFloat(
        initialValue  = 14f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(590, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "rEq2"
    )
    val eqB3 by infiniteTransition.animateFloat(
        initialValue  = 9f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "rEq3"
    )

    Surface(
        modifier      = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        shape         = RoundedCornerShape(12.dp),
        color         = if (isCurrent)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else
            Color.Transparent,
        border        = if (isCurrent)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        else null,
        tonalElevation= if (isCurrent) 1.dp else 0.dp
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Index / playing indicator
            Box(
                modifier         = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    Row(
                        verticalAlignment     = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf(eqB1, eqB2, eqB3).forEach { h ->
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(h.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                } else {
                    Text(
                        text      = "${index + 1}",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = song.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color      = if (isCurrent)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = song.artist,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isCurrent) {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(
                        onClick  = onPlayNext,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Play next",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick  = onRemove,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.RemoveCircleOutline,
                            contentDescription = "Remove from queue",
                            tint               = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Empty / Error States ─────────────────────────────────────────────────────

@Composable
private fun NowPlayingEmptyState() {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier      = Modifier.size(96.dp),
            shape         = CircleShape,
            color         = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            border        = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector       = Icons.Default.MusicNote,
                    contentDescription= null,
                    tint              = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier          = Modifier.size(46.dp)
                )
            }
        }
        Text(
            text      = "Nothing playing",
            style     = MaterialTheme.typography.headlineSmall,
            fontWeight= FontWeight.Bold,
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text      = "Choose a song from Home, Library\nor Search to start listening",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight= 22.sp
        )
    }
}

@Composable
private fun NowPlayingErrorState(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier            = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector       = Icons.Default.ErrorOutline,
            contentDescription= null,
            tint              = MaterialTheme.colorScheme.error,
            modifier          = Modifier.size(54.dp)
        )
        Text(
            "Playback error",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Text(
            message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Reusable Controls ────────────────────────────────────────────────────────

@Composable
private fun TransportButton(
    icon              : ImageVector,
    contentDescription: String,
    size              : Dp,
    onClick           : () -> Unit
) {
    Surface(
        onClick       = onClick,
        modifier      = Modifier.size(size),
        shape         = CircleShape,
        color         = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border        = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        tonalElevation= 2.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector       = icon,
                contentDescription= contentDescription,
                tint              = MaterialTheme.colorScheme.onSurface,
                modifier          = Modifier.size((size.value * 0.50f).dp)
            )
        }
    }
}

@Composable
private fun ToggleIconButton(
    icon              : ImageVector,
    active            : Boolean,
    size              : Dp,
    contentDescription: String,
    onClick           : () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier= Modifier.size(size),
        shape   = CircleShape,
        color   = if (active)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.Transparent,
        border  = if (active)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        else null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector       = icon,
                contentDescription= contentDescription,
                tint              = if (active)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                modifier          = Modifier.size((size.value * 0.60f).dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Text(
            label,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(68.dp)
        )
        Text(
            value,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}