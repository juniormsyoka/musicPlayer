package com.vybzvault.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.vybzvault.music.equalizer.EqPreset
import com.vybzvault.music.home.HomeUiState
import com.vybzvault.music.ui.player.MiniPlayer
import com.vybzvault.music.ui.screens.EqualizerScreen
import com.vybzvault.music.ui.screens.HomeScreen
import com.vybzvault.music.ui.screens.LibraryScreen
import com.vybzvault.music.ui.screens.NowPlayingScreen
import com.vybzvault.music.ui.screens.SearchScreen
import com.vybzvault.music.ui.screens.SettingsScreen
import com.vybzvault.music.ui.theme.AppThemePreset
import com.vybzvault.music.ui.AnimatedNavigationBar
import com.vybzvault.music.ui.ModernNavData
import kotlinx.coroutines.launch

/**
 * Reconstructed Internal Navigation Schema matching the unified single-icon tracking paradigm.
 */
private sealed class NavigationItem(
    val id: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : NavigationItem("home", "Home", Icons.Filled.Home)
    data object Search : NavigationItem("search", "Search", Icons.Filled.Search)
    data object Library : NavigationItem("library", "Library", Icons.Filled.LibraryMusic)
    data object Player : NavigationItem("player", "Playing", Icons.Filled.PlayArrow)
    data object Settings : NavigationItem("settings", "Settings", Icons.Filled.Settings)
    data object Equalizer : NavigationItem("equalizer", "Equalizer", Icons.Filled.Tune)
}

@Composable
fun MusicAppContent(
    libraryState: LibraryState,
    homeUiState: HomeUiState,
    playbackState: PlaybackState,
    selectedSongIds: Set<Long>,
    searchQuery: String,
    searchFilter: SearchFilter,
    selectedFolder: String?,
    sortOption: LibrarySortOption,
    audioSettingsState: AudioSettingsState,
    selectedTheme: AppThemePreset,
    formatTime: (Long) -> String,
    equalizerState: EqualizerState,
    presets: List<SettingsPreset>,
    selectedPresetId: String?,
    events: MusicAppEvents,
    onSearchQueryChange: (String) -> Unit,
    onSearchFilterChange: (SearchFilter) -> Unit,
    onClearRecentSearches: () -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onAddSongToPlaylist: (String, Long) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onQuickResume: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onUpdateQueue: (List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onSetRepeatMode: (RepeatMode) -> Unit,
    onSetTheme: (AppThemePreset) -> Unit,
    onSetShowAlbumArt: (Boolean) -> Unit,
    onSetHighResEnabled: (Boolean) -> Unit,
    onSetCrossfadeSeconds: (Int) -> Unit,
    onToggleEqualizerEnabled: (Boolean) -> Unit,
    onSetEqPreset: (EqPreset) -> Unit,
    onSetEqPreamp: (Float) -> Unit,
    onSetEqBandLevel: (Int, Int) -> Unit,
    onSelectPreset: (String) -> Unit,
    onSaveCurrentAsPreset: () -> Unit,
    onResetToDefaults: () -> Unit,
    isUserSignedIn: Boolean
) {
    val playbackEvents = events.playback

    // Define core persistent bottom bar tab entries
    val navigationItems = remember {
        listOf(
            NavigationItem.Home,
            NavigationItem.Search,
            NavigationItem.Library,
            NavigationItem.Player,
            NavigationItem.Settings
        )
    }

    // Map internal screens cleanly to the package com.vybzvault.music.ui external structural types
    val automatedNavDataList = remember(navigationItems) {
        navigationItems.map { item ->
            ModernNavData(
                id = item.id,
                title = item.title,
                icon = item.icon
            )
        }
    }

    var selectedItem by remember { mutableStateOf<NavigationItem>(NavigationItem.Home) }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    val onPlayNextWithFeedback: (Song) -> Unit = { song ->
        onPlayNext(song)
        snackbarScope.launch {
            snackbarHostState.showSnackbar("${song.title} will play next")
        }
    }

    val currentSong = when (val state = playbackState.playerState) {
        is PlayerState.Ready -> state.song
        is PlayerState.Playing -> state.song
        is PlayerState.Paused -> state.song
        else -> null
    }

    val filteredSongs = getFilteredSongs(libraryState, searchQuery, searchFilter)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Color.Transparent) // Keeps the background clear for your island-style dock
                    .navigationBarsPadding()       // Protects against device system gesture bars
            ) {
                // ✅ The condition now ONLY hides the MiniPlayer when the main player screen is open
                if (selectedItem != NavigationItem.Player && currentSong != null) {
                    MiniPlayer(
                        playbackState = playbackState,
                        showAlbumArt = audioSettingsState.showAlbumArt,
                        onTogglePlayPause = playbackEvents.onTogglePlayPause,
                        onSkipNext = playbackEvents.onSkipNext,
                        onOpenNowPlaying = { selectedItem = NavigationItem.Player }
                    )
                }

                // ✅ This sits outside the condition, so it stays permanently visible!
                AnimatedNavigationBar(
                    items = automatedNavDataList,
                    selectedId = selectedItem.id,
                    onItemSelected = { targetId ->
                        val match = navigationItems.find { it.id == targetId }
                        if (match != null) {
                            selectedItem = match
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when (selectedItem) {
                NavigationItem.Home -> HomeScreen(
                    homeUiState = homeUiState,
                    showAlbumArt = audioSettingsState.showAlbumArt,
                    onPlaySong = onPlaySong,
                    onQuickResume = onQuickResume,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )

                NavigationItem.Search -> SearchScreen(
                    libraryState = libraryState,
                    searchQuery = searchQuery,
                    showAlbumArt = audioSettingsState.showAlbumArt,
                    searchFilter = searchFilter,
                    selectedSongIds = selectedSongIds,
                    filteredSongs = filteredSongs,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchFilterChange = onSearchFilterChange,
                    onPlaySong = onPlaySong,
                    onPlayNext = onPlayNextWithFeedback,
                    onClearRecentSearches = onClearRecentSearches,
                    onRemoveRecentSearch = onRemoveRecentSearch,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )

                NavigationItem.Library -> LibraryScreen(
                    libraryState = libraryState,
                    selectedFolder = selectedFolder,
                    showAlbumArt = audioSettingsState.showAlbumArt,
                    sortOption = sortOption,
                    selectedSongIds = selectedSongIds,
                    currentPlayingSongId = currentSong?.id,
                    onFolderSelected = onFolderSelected,
                    onSortOptionChange = onSortOptionChange,
                    onCreatePlaylist = onCreatePlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onRenamePlaylist = onRenamePlaylist,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                    onPlaySong = onPlaySong,
                    onPlayNext = onPlayNextWithFeedback,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSongSelection = onToggleSongSelection
                )

                NavigationItem.Player -> NowPlayingScreen(
                    playbackState = playbackState,
                    showAlbumArt = audioSettingsState.showAlbumArt,
                    formatTime = formatTime,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onSeekTo = onSeekTo,
                    onToggleShuffle = onToggleShuffle,
                    onSetRepeatMode = onSetRepeatMode,
                    onUpdateQueue = onUpdateQueue,
                    onPlaySong = onPlaySong,
                    onPlayNext = onPlayNextWithFeedback,
                    onOpenEqualizer = { selectedItem = NavigationItem.Equalizer }
                )

                NavigationItem.Settings -> SettingsScreen(
                    audioSettingsState = audioSettingsState,
                    selectedTheme = selectedTheme,
                    onSetTheme = onSetTheme,
                    onOpenEqualizer = { selectedItem = NavigationItem.Equalizer },
                    onSetShowAlbumArt = onSetShowAlbumArt,
                    onSetHighResEnabled = onSetHighResEnabled,
                    onSetCrossfadeSeconds = onSetCrossfadeSeconds,
                    presets = presets,
                    selectedPresetId = selectedPresetId,
                    onSelectPreset = onSelectPreset,
                    onSaveCurrentAsPreset = onSaveCurrentAsPreset,
                    onResetToDefaults = onResetToDefaults,
                    isUserSignedIn = isUserSignedIn
                )

                NavigationItem.Equalizer -> EqualizerScreen(
                    equalizerState = equalizerState,
                    onBack = { selectedItem = NavigationItem.Settings },
                    onToggleEnabled = onToggleEqualizerEnabled,
                    onSetPreset = onSetEqPreset,
                    onSetPreamp = onSetEqPreamp,
                    onSetBandLevel = onSetEqBandLevel
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