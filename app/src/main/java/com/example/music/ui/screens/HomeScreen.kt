package com.example.music.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.music.LibraryState
import com.example.music.Song
import com.example.music.ui.components.AlbumArt
import java.util.Calendar

@Composable
fun HomeScreen(
    libraryState: LibraryState,
    selectedSongIds: Set<Long>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
) {
    if (libraryState.isLoading && libraryState.songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (libraryState.songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No songs found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Try adding music files to your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val recentSongs = remember(libraryState.songs) {
        libraryState.songs
            .sortedByDescending { it.dateAddedSec }
            .take(6)
    }
    val mostPlayedSongs = remember(libraryState.songs, libraryState.playCounts) {
        libraryState.songs
            .sortedByDescending { libraryState.playCounts[it.id] ?: 0 }
            .filter { (libraryState.playCounts[it.id] ?: 0) > 0 }
            .take(6)
    }
    val favoriteSongs = remember(libraryState.songs, libraryState.favorites) {
        libraryState.songs
            .filter { libraryState.favorites.contains(it.id) }
            .take(6)
    }
    val repeatHits = remember(libraryState.songs, libraryState.playCounts) {
        libraryState.songs
            .filter { (libraryState.playCounts[it.id] ?: 0) >= 3 }
            .sortedByDescending { libraryState.playCounts[it.id] ?: 0 }
            .take(6)
    }
    val hiddenGems = remember(libraryState.songs, libraryState.playCounts) {
        libraryState.songs
            .filter { (libraryState.playCounts[it.id] ?: 0) in 0..2 }
            .sortedByDescending { it.dateAddedSec }
            .take(6)
    }

    val nowHour = remember { currentHour24() }

    val recommendation = remember(libraryState.songs, libraryState.playCounts, libraryState.favorites, nowHour) {
        buildTimeBasedRecommendation(
            songs = libraryState.songs,
            playCounts = libraryState.playCounts,
            favorites = libraryState.favorites,
            nowHour = nowHour
        )
    }

    val totalPlays = remember(libraryState.playCounts) { libraryState.playCounts.values.sum() }
    val greeting = remember(nowHour) { greetingForHour(nowHour) }
    val contextualLine = remember(libraryState.songs, libraryState.favorites, totalPlays, greeting) {
        "$greeting, you have ${libraryState.songs.size} songs, ${libraryState.favorites.size} favorites, and $totalPlays total plays."
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            HomeContextHeader(
                greeting = greeting,
                contextualLine = contextualLine
            )
        }

        if (recommendation.songs.isNotEmpty()) {
            item {
                HomeSongGridSection(
                    title = recommendation.title,
                    subtitle = recommendation.subtitle,
                    songs = recommendation.songs,
                    favorites = libraryState.favorites,
                    selectedSongIds = selectedSongIds,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )
            }
        }

        if (recentSongs.isNotEmpty()) {
            item {
                HomeSongGridSection(
                    title = "Recently Added",
                    subtitle = "${recentSongs.size} songs",
                    songs = recentSongs,
                    favorites = libraryState.favorites,
                    selectedSongIds = selectedSongIds,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )
            }
        }

        if (mostPlayedSongs.isNotEmpty()) {
            item {
                HomeSongGridSection(
                    title = "Most Played",
                    subtitle = "${mostPlayedSongs.size} songs",
                    songs = mostPlayedSongs,
                    favorites = libraryState.favorites,
                    selectedSongIds = selectedSongIds,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )
            }
        }

        if (repeatHits.isNotEmpty()) {
            item {
                HomeSongGridSection(
                    title = "On Repeat",
                    subtitle = "Your most replayed tracks",
                    songs = repeatHits,
                    favorites = libraryState.favorites,
                    selectedSongIds = selectedSongIds,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )
            }
        }

        if (hiddenGems.isNotEmpty()) {
            item {
                HomeSongGridSection(
                    title = "Hidden Gems",
                    subtitle = "Songs you have barely explored",
                    songs = hiddenGems,
                    favorites = libraryState.favorites,
                    selectedSongIds = selectedSongIds,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )
            }
        }

        if (favoriteSongs.isNotEmpty()) {
            item {
                HomeSongGridSection(
                    title = "Favorites",
                    subtitle = "${favoriteSongs.size} songs",
                    songs = favoriteSongs,
                    favorites = libraryState.favorites,
                    selectedSongIds = selectedSongIds,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun HomeContextHeader(
    greeting: String,
    contextualLine: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = contextualLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class TimeBasedRecommendation(
    val title: String,
    val subtitle: String,
    val songs: List<Song>
)

private fun currentHour24(): Int {
    return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

private fun buildTimeBasedRecommendation(
    songs: List<Song>,
    playCounts: Map<Long, Int>,
    favorites: Set<Long>,
    nowHour: Int
): TimeBasedRecommendation {
    val songsWithCount = songs.map { it to (playCounts[it.id] ?: 0) }

    return when (nowHour) {
        in 5..11 -> {
            val picks = songsWithCount
                .sortedWith(compareBy<Pair<Song, Int>> { it.second }.thenByDescending { it.first.dateAddedSec })
                .map { it.first }
                .take(6)
            TimeBasedRecommendation(
                title = "Morning Discovery",
                subtitle = "Fresh picks to start your day",
                songs = picks
            )
        }

        in 12..16 -> {
            val picks = songsWithCount
                .filter { (_, count) -> count in 1..8 }
                .sortedByDescending { it.second }
                .map { it.first }
                .ifEmpty {
                    songsWithCount.sortedByDescending { it.second }.map { it.first }
                }
                .take(6)
            TimeBasedRecommendation(
                title = "Afternoon Focus",
                subtitle = "Steady tracks for your flow",
                songs = picks
            )
        }

        else -> {
            val picks = songsWithCount
                .sortedWith(
                    compareByDescending<Pair<Song, Int>> { favorites.contains(it.first.id) }
                        .thenByDescending { it.second }
                )
                .map { it.first }
                .take(6)
            TimeBasedRecommendation(
                title = "Evening Favorites",
                subtitle = "Wind down with your best-loved tracks",
                songs = picks
            )
        }
    }
}

@Composable
private fun HomeSongGridSection(
    title: String,
    subtitle: String,
    songs: List<Song>,
    favorites: Set<Long>,
    selectedSongIds: Set<Long>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        songs.chunked(2).forEach { rowSongs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowSongs.forEach { song ->
                    HomeSongGridCard(
                        song = song,
                        queue = songs,
                        isFavorite = favorites.contains(song.id),
                        isSelected = selectedSongIds.contains(song.id),
                        onPlaySong = onPlaySong,
                        onToggleFavorite = onToggleFavorite,
                        onToggleSongSelection = onToggleSongSelection,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowSongs.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HomeSongGridCard(
    song: Song,
    queue: List<Song>,
    isFavorite: Boolean,
    isSelected: Boolean,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onPlaySong(song, queue) },
                onLongClick = { onToggleSongSelection(song.id) }
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AlbumArt(
                uri = song.albumArtUri,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { onToggleFavorite(song.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}



