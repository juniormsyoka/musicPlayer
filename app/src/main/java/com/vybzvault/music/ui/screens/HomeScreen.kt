package com.vybzvault.music.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vybzvault.music.Song
import com.vybzvault.music.home.HomeSectionUiModel
import com.vybzvault.music.home.HomeStreakUiModel
import com.vybzvault.music.home.HomeUiState
import com.vybzvault.music.home.ResumeSession
import com.vybzvault.music.ui.components.AlbumArt
import kotlin.math.min


// ─── Home Screen ──────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    homeUiState          : HomeUiState,
    showAlbumArt         : Boolean,
    onPlaySong           : (Song, List<Song>) -> Unit,
    onQuickResume        : () -> Unit,
    onToggleFavorite     : (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
) {
    when {
        homeUiState.isLoading      -> LoadingContent()
        homeUiState.isEmptyLibrary -> EmptyLibraryContent()
        else                       -> HomeContent(
            homeUiState           = homeUiState,
            showAlbumArt          = showAlbumArt,
            onPlaySong            = onPlaySong,
            onQuickResume         = onQuickResume,
            onToggleFavorite      = onToggleFavorite,
            onToggleSongSelection = onToggleSongSelection
        )
    }
}

@Composable
private fun HomeContent(
    homeUiState          : HomeUiState,
    showAlbumArt         : Boolean,
    onPlaySong           : (Song, List<Song>) -> Unit,
    onQuickResume        : () -> Unit,
    onToggleFavorite     : (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding      = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier            = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Header
        item {
            HeroHeader(
                greeting             = homeUiState.greeting,
                contextualLine       = homeUiState.contextualLine,
                streak               = homeUiState.streak,
                weeklyDiscoveryCount = homeUiState.weeklyDiscoveryCount
            )
        }

        // Continue Listening card
        homeUiState.resumeSession?.let { session ->
            item {
                Spacer(Modifier.height(4.dp))
                QuickResumeCard(session = session, onResume = onQuickResume)
            }
        }

        // Selection banner
        if (homeUiState.selectionCount > 0) {
            item {
                Spacer(Modifier.height(12.dp))
                SelectionBanner(selectionCount = homeUiState.selectionCount)
            }
        }

        // Dynamic sections
        homeUiState.sections.forEach { section ->
            item(section.key) {
                Spacer(Modifier.height(24.dp))
                SongSection(
                    section               = section,
                    favorites             = homeUiState.favoriteSongIds,
                    showAlbumArt          = showAlbumArt,
                    selectedSongIds       = homeUiState.selectedSongIds,
                    onPlaySong            = onPlaySong,
                    onToggleFavorite      = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )
            }
        }
    }
}

// ─── Hero Header ──────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(
    greeting            : String,
    contextualLine      : String,
    streak              : HomeStreakUiModel,
    weeklyDiscoveryCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text         = greeting,
                        fontSize     = 30.sp,
                        fontWeight   = FontWeight.Bold,
                        color        = MaterialTheme.colorScheme.onBackground,
                        letterSpacing= (-0.5).sp,
                        lineHeight   = 34.sp
                    )
                    Spacer(Modifier.height(6.dp))

                    // Discovery badge
                    Row(
                        modifier          = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector       = Icons.Filled.FiberNew,
                            contentDescription= null,
                            tint              = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier          = Modifier.size(14.dp)
                        )
                        Text(
                            text  = "$weeklyDiscoveryCount new this week",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))

            }

            Spacer(Modifier.height(18.dp))

            // Contextual quote card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector       = Icons.Filled.FormatQuote,
                        contentDescription= null,
                        tint              = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier          = Modifier.size(20.dp)
                    )
                }
                Text(
                    text      = contextualLine,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier  = Modifier.weight(1f),
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis
                )
            }
        }
    }
}


// ─── Quick Resume Card ────────────────────────────────────────────────────────

@Composable
private fun QuickResumeCard(
    session : ResumeSession,
    onResume: () -> Unit
) {
    val cardColors = CardDefaults.cardColors(containerColor = Color.Transparent)
    val gradientColors = listOf(
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    )

    Card(
        onClick   = onResume,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = cardColors,
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors = gradientColors))
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Play badge
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector       = Icons.Filled.PlayArrow,
                        contentDescription= null,
                        tint              = MaterialTheme.colorScheme.onPrimary,
                        modifier          = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text          = "CONTINUE LISTENING",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = session.lastPlayedSong.title,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text     = "${session.lastPlayedSong.artist}  ·  ${formatResumePosition(session.positionSeconds)}",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector       = Icons.Filled.SkipNext,
                    contentDescription= "Resume",
                    tint              = MaterialTheme.colorScheme.primary,
                    modifier          = Modifier.size(26.dp)
                )
            }
        }
    }
}

// ─── Song Section ─────────────────────────────────────────────────────────────

@Composable
private fun SongSection(
    section              : HomeSectionUiModel,
    favorites            : Set<Long>,
    showAlbumArt         : Boolean,
    selectedSongIds      : Set<Long>,
    onPlaySong           : (Song, List<Song>) -> Unit,
    onToggleFavorite     : (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector       = sectionIcon(section.title),
                        contentDescription= null,
                        tint              = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier          = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text       = section.title,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text  = section.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


        }

        Spacer(Modifier.height(12.dp))

        if (section.songs.isEmpty()) {
            EmptySectionMessage(message = section.emptyMessage)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding        = PaddingValues(horizontal = 20.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                items(section.songs, key = { it.id }) { song ->
                    WarmSongCard(
                        song                  = song,
                        queue                 = section.songs,
                        isFavorite            = favorites.contains(song.id),
                        showAlbumArt          = showAlbumArt,
                        isSelectionMode       = selectedSongIds.isNotEmpty(),
                        isSelected            = selectedSongIds.contains(song.id),
                        onPlaySong            = onPlaySong,
                        onToggleFavorite      = onToggleFavorite,
                        onToggleSongSelection = onToggleSongSelection
                    )
                }
            }
        }
    }
}

// ─── Song Card ────────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WarmSongCard(
    song                 : Song,
    queue                : List<Song>,
    isFavorite           : Boolean,
    showAlbumArt         : Boolean,
    isSelectionMode      : Boolean,
    isSelected           : Boolean,
    onPlaySong           : (Song, List<Song>) -> Unit,
    onToggleFavorite     : (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
    modifier             : Modifier = Modifier
) {
    val screenWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val cardWidth   = ((screenWidth - 40.dp) / 2.2f).coerceAtMost(180.dp)
    val cardShape   = RoundedCornerShape(18.dp)

    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier
            .width(cardWidth)
            .combinedClickable(
                onClick     = { if (isSelectionMode) onToggleSongSelection(song.id) else onPlaySong(song, queue) },
                onLongClick = { onToggleSongSelection(song.id) }
            ),
        shape     = cardShape,
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        border    = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Album art area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                AlbumArt(
                    uri         = song.albumArtUri,
                    modifier    = Modifier.fillMaxSize(),
                    showArtwork = showAlbumArt
                )

                // Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors   = listOf(Color.Transparent, Color(0x99000000)),
                                startY   = Float.MAX_VALUE * 0.35f
                            )
                        )
                )

                // Favorite / selection badge
                Box(
                    modifier         = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFavorite || isSelected) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = .35f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isSelectionMode && isSelected -> Icon(
                            imageVector       = Icons.Filled.Check,
                            contentDescription= null,
                            tint              = MaterialTheme.colorScheme.onPrimary,
                            modifier          = Modifier.size(16.dp)
                        )
                        !isSelectionMode -> {
                            val iconRes = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
                            IconButton(
                                onClick  = { onToggleFavorite(song.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector       = iconRes,
                                    contentDescription= "Favorite",
                                    tint              = Color.White,
                                    modifier          = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Central play circle
                Box(
                    modifier         = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .90f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector       = Icons.Filled.PlayArrow,
                        contentDescription= "Play",
                        tint              = MaterialTheme.colorScheme.onPrimary,
                        modifier          = Modifier.size(26.dp)
                    )
                }
            }

            // Text info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = song.title,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = song.artist,
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.durationMs > 0L) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector       = Icons.Filled.AccessTime,
                            contentDescription= null,
                            tint              = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier          = Modifier.size(10.dp)
                        )
                        Text(
                            text  = formatDuration(song.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─── Selection Banner ─────────────────────────────────────────────────────────

@Composable
private fun SelectionBanner(selectionCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector       = Icons.Filled.Checklist,
            contentDescription= null,
            tint              = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier          = Modifier.size(20.dp)
        )
        Text(
            text       = "$selectionCount song${if (selectionCount > 1) "s" else ""} selected",
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp,
            color      = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier   = Modifier.weight(1f)
        )
        Text(
            text  = "Long-press to add more",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Empty States ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
                strokeWidth = 3.dp
            )
            Text(
                text  = "Loading your music…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLibraryContent() {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier            = Modifier.padding(40.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector       = Icons.Filled.MusicNote,
                    contentDescription= null,
                    tint              = MaterialTheme.colorScheme.primary,
                    modifier          = Modifier.size(44.dp)
                )
            }
            Text(
                text       = "Your library is empty",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
                textAlign  = TextAlign.Center
            )
            Text(
                text      = "Add music files to your device\nto start listening",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight= 22.sp
            )
        }
    }
}

@Composable
private fun EmptySectionMessage(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector       = Icons.Filled.Info,
            contentDescription= null,
            tint              = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
            modifier          = Modifier.size(18.dp)
        )
        Text(
            text  = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun sectionIcon(title: String) = when {
    title.contains("recent",    ignoreCase = true) -> Icons.Filled.History
    title.contains("favorite",  ignoreCase = true) -> Icons.Filled.Favorite
    title.contains("discover",  ignoreCase = true) -> Icons.Filled.Explore
    title.contains("top",       ignoreCase = true) -> Icons.Filled.TrendingUp
    title.contains("playlist",  ignoreCase = true) -> Icons.Filled.QueueMusic
    else                                            -> Icons.Filled.MusicNote
}

private fun Modifier.shapeBorder(width: androidx.compose.ui.unit.Dp, color: Color, shape: Shape) =
    border(width, color, shape)

private fun formatResumePosition(positionSeconds: Int): String {
    val m = positionSeconds / 60
    val s = positionSeconds % 60
    return if (m >= 60) "%d:%02d:%02d".format(m / 60, m % 60, s)
    else "%d:%02d".format(m, s)
}

private fun formatDuration(durationMs: Long): String {
    val t = (durationMs / 1000L).toInt()
    val m = t / 60
    val s = t % 60
    return "%d:%02d".format(m, s)
}