@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.music.ui.player.MiniPlayer
import com.example.music.ui.screens.HomeScreen
import com.example.music.ui.screens.LibraryScreen
import com.example.music.ui.screens.NowPlayingScreen
import com.example.music.ui.screens.SearchScreen

private sealed class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : NavigationItem("Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Search : NavigationItem("Search", Icons.Filled.Search, Icons.Outlined.Search)
    data object Library : NavigationItem("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
    data object Player : NavigationItem("Now Playing", Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow)
}

@Composable
fun MusicAppContent(
    libraryState: LibraryState,
    playbackState: PlaybackState,
    selectedSongIds: Set<Long>,
    searchQuery: String,
    searchFilter: SearchFilter,
    selectedFolder: String?,
    sortOption: LibrarySortOption,
    onSearchQueryChange: (String) -> Unit,
    onSearchFilterChange: (SearchFilter) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onAddSongToPlaylist: (String, Long) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onUpdateQueue: (List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
    volumePercent: Int,
    onSetVolumePercent: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onSetRepeatMode: (RepeatMode) -> Unit,
    formatTime: (Long) -> String,
) {
    val navigationItems = listOf(
        NavigationItem.Home,
        NavigationItem.Search,
        NavigationItem.Library,
        NavigationItem.Player
    )

    var selectedItem by remember { mutableStateOf<NavigationItem>(NavigationItem.Home) }
    val currentSong = currentSong(playbackState.playerState)
    val filteredSongs by remember(
        libraryState.songs,
        libraryState.favorites,
        libraryState.playCounts,
        searchQuery,
        searchFilter
    ) {
        derivedStateOf {
            getFilteredSongs(
                libraryState = libraryState,
                searchQuery = searchQuery,
                searchFilter = searchFilter
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(modifier = androidx.compose.ui.Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Mini player above nav bar — only when not on player screen
                if (selectedItem != NavigationItem.Player && currentSong != null) {
                    MiniPlayer(
                        playbackState = playbackState,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onOpenNowPlaying = { selectedItem = NavigationItem.Player }
                    )
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = selectedItem == item,
                            onClick = { selectedItem = item },
                            icon = {
                                Icon(
                                    imageVector = if (selectedItem == item) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { androidx.compose.material3.Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = androidx.compose.ui.Modifier.padding(paddingValues)) {
            when (selectedItem) {
                NavigationItem.Home -> HomeScreen(
                    libraryState = libraryState,
                    selectedSongIds = selectedSongIds,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )

                NavigationItem.Search -> SearchScreen(
                    libraryState = libraryState,
                    searchQuery = searchQuery,
                    searchFilter = searchFilter,
                    selectedSongIds = selectedSongIds,
                    filteredSongs = filteredSongs,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchFilterChange = onSearchFilterChange,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )

                NavigationItem.Library -> LibraryScreen(
                    libraryState = libraryState,
                    selectedFolder = selectedFolder,
                    sortOption = sortOption,
                    selectedSongIds = selectedSongIds,
                    onFolderSelected = onFolderSelected,
                    onSortOptionChange = onSortOptionChange,
                    onCreatePlaylist = onCreatePlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onRenamePlaylist = onRenamePlaylist,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )

                NavigationItem.Player -> NowPlayingScreen(
                    playbackState = playbackState,
                    volumePercent = volumePercent,
                    formatTime = formatTime,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onSeekTo = onSeekTo,
                    onToggleShuffle = onToggleShuffle,
                    onSetRepeatMode = onSetRepeatMode,
                    onSetVolumePercent = onSetVolumePercent,
                    onUpdateQueue = onUpdateQueue,
                    onPlaySong = onPlaySong
                )
            }
        }
    }
}

private fun getFilteredSongs(
    libraryState: LibraryState,
    searchQuery: String,
    searchFilter: SearchFilter
): List<UiSong> {
    val query = searchQuery.lowercase().trim()
    if (query.isBlank()) return emptyList()

    val filtered = libraryState.songs.filter { song ->
        when (searchFilter) {
            SearchFilter.SONG -> song.title.contains(query, ignoreCase = true)
            SearchFilter.ARTIST -> song.artist.contains(query, ignoreCase = true)
            SearchFilter.ALBUM -> song.album.contains(query, ignoreCase = true)
        }
    }

    return filtered.map {
        UiSong(
            song = it,
            isFavorite = libraryState.favorites.contains(it.id),
            playCount = libraryState.playCounts[it.id] ?: 0
        )
    }
}