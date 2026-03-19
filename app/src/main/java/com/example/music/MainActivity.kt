@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.music

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.music.ui.theme.MusicTheme


class MainActivity : ComponentActivity() {

    private val playbackViewModel: PlaybackViewModel by viewModels {
        PlaybackViewModelFactory(application)
    }

    private val libraryViewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Sonora_Splash)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicPlayerApp(
                        playbackViewModel = playbackViewModel,
                        libraryViewModel = libraryViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicPlayerApp(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op: playback still works, but notification visibility depends on user choice */ }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            libraryViewModel.refreshLibrary()
            requestNotificationPermissionIfNeeded(context, notificationPermissionLauncher::launch)
        }
    }

    LaunchedEffect(Unit) {
        if (hasAudioPermission(context)) {
            libraryViewModel.refreshLibrary()
            requestNotificationPermissionIfNeeded(context, notificationPermissionLauncher::launch)
        } else {
            audioPermissionLauncher.launch(requiredPermission())
        }
    }

    // Collect states
    val libraryState by libraryViewModel.state.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()
    val selectedSongIds by libraryViewModel.selectedSongIds.collectAsState()
    val sortOption by libraryViewModel.sortOption.collectAsState()
    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    val searchFilter by libraryViewModel.searchFilter.collectAsState()
    val selectedFolder by libraryViewModel.selectedFolder.collectAsState()
    val volumePercent by playbackViewModel.volumePercent.collectAsState()

    MusicAppContent(
        libraryState = libraryState,
        playbackState = playbackState,
        selectedSongIds = selectedSongIds,
        searchQuery = searchQuery,
        searchFilter = searchFilter,
        selectedFolder = selectedFolder,
        sortOption = sortOption,
        onSearchQueryChange = libraryViewModel::updateSearchQuery,
        onSearchFilterChange = libraryViewModel::setSearchFilter,
        onFolderSelected = libraryViewModel::setSelectedFolder,
        onSortOptionChange = libraryViewModel::setSortOption,
        onCreatePlaylist = libraryViewModel::createPlaylist,
        onDeletePlaylist = libraryViewModel::deletePlaylist,
        onRenamePlaylist = libraryViewModel::renamePlaylist,
        onAddSongToPlaylist = libraryViewModel::addSongToPlaylist,
        onRemoveSongFromPlaylist = libraryViewModel::removeSongFromPlaylist,
        onPlaySong = { song, queue ->
            playbackViewModel.playSong(song, queue)
        },
        onTogglePlayPause = playbackViewModel::togglePlayPause,
        onSkipNext = playbackViewModel::skipNext,
        onSkipPrevious = playbackViewModel::skipPrevious,
        onSeekTo = playbackViewModel::seekTo,
        onUpdateQueue = playbackViewModel::updateQueue,
        onToggleFavorite = libraryViewModel::toggleFavorite,
        onToggleSongSelection = libraryViewModel::toggleSongSelection,
        volumePercent = volumePercent,
        onSetVolumePercent = playbackViewModel::setVolumePercent,
        onToggleShuffle = playbackViewModel::toggleShuffle,
        onSetRepeatMode = playbackViewModel::setRepeatMode,
        formatTime = playbackViewModel::formatTime,
    )
}

private fun requestNotificationPermissionIfNeeded(
    context: Context,
    request: (String) -> Unit = {}
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (hasNotificationPermission(context)) return
    request(Manifest.permission.POST_NOTIFICATIONS)
}

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasAudioPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        requiredPermission()
    ) == PackageManager.PERMISSION_GRANTED
}

private fun requiredPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}