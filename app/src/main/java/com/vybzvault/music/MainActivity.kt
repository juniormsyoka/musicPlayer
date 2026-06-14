@file:OptIn(
	androidx.compose.foundation.ExperimentalFoundationApi::class,
	androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.vybzvault.music
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.vybzvault.music.ui.theme.AppThemePreset
import com.vybzvault.music.ui.theme.MusicTheme


class MainActivity : ComponentActivity() {

	private val playbackViewModel: PlaybackViewModel by viewModels {
		PlaybackViewModelFactory(application)
	}

	private val libraryViewModel: LibraryViewModel by viewModels {
		LibraryViewModelFactory(application)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
	//	setTheme(R.style.Theme_Sonora_Splash)
		super.onCreate(savedInstanceState)

		enableEdgeToEdge()
		WindowCompat.setDecorFitsSystemWindows(window, false)
		StateManager.initialize(applicationContext)

		setContent {
			val themeStore = remember { ThemeStore(applicationContext) }
			var selectedTheme by rememberSaveable { mutableStateOf(themeStore.load()) }
			MusicTheme(themePreset = selectedTheme) {
				Surface(
					modifier = Modifier.fillMaxSize(),
					color = MaterialTheme.colorScheme.background
				) {
					MusicPlayerApp(
						playbackViewModel = playbackViewModel,
						libraryViewModel = libraryViewModel,
						selectedTheme = selectedTheme,
						onSetTheme = { newTheme ->
							selectedTheme = newTheme
							themeStore.save(newTheme)
						}
					)
				}
			}
		}
	}

	override fun onStop() {
		super.onStop()
		playbackViewModel.savePlaybackStateSnapshot()
		libraryViewModel.refreshHomeSessionData()
	}
}

@Composable
private fun MusicPlayerApp(
	playbackViewModel: PlaybackViewModel,
	libraryViewModel: LibraryViewModel,
	selectedTheme: AppThemePreset,
	onSetTheme: (AppThemePreset) -> Unit
) {
	val context = LocalContext.current
	val audioSettingsStore = remember(context) { AudioSettingsStore(context.applicationContext) }

	val notificationPermissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) {
		// Playback works even when notification permission is denied.
	}

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

	val libraryState by libraryViewModel.state.collectAsState()
	val homeUiState by libraryViewModel.homeUiState.collectAsState()
	val playbackState by playbackViewModel.state.collectAsState()
	val selectedSongIds by libraryViewModel.selectedSongIds.collectAsState()
	val sortOption by libraryViewModel.sortOption.collectAsState()
	val searchQuery by libraryViewModel.searchQuery.collectAsState()
	val searchFilter by libraryViewModel.searchFilter.collectAsState()
	val selectedFolder by libraryViewModel.selectedFolder.collectAsState()
	val equalizerState by playbackViewModel.equalizerState.collectAsState()

	var audioSettingsState by remember { mutableStateOf(audioSettingsStore.load()) }
	val templatePresets = remember { builtInSettingsPresets() }
	val customPresets = remember { mutableStateListOf<SettingsPreset>() }
	var selectedPresetId by rememberSaveable { mutableStateOf<String?>(null) }
	var customPresetCounter by rememberSaveable { mutableStateOf(1) }
	val allPresets = templatePresets + customPresets

	LaunchedEffect(libraryState.songs) {
		playbackViewModel.tryRestorePlayback(libraryState.songs)
	}

	LaunchedEffect(audioSettingsState) {
		audioSettingsStore.save(audioSettingsState)
	}

	fun applyPreset(id: String) {
		allPresets.firstOrNull { it.id == id }?.let { preset ->
			audioSettingsState = preset.audioSettings
			preset.themePreset?.let(onSetTheme)
			selectedPresetId = preset.id
		}
	}

	val musicAppEvents = MusicAppEvents.Default(
		library = LibraryEvents(
			onSearchQueryChange = libraryViewModel::updateSearchQuery,
			onSearchFilterChange = libraryViewModel::setSearchFilter,
			onClearRecentSearches = libraryViewModel::clearRecentSearches,
			onRemoveRecentSearch = libraryViewModel::removeRecentSearch,
			onFolderSelected = libraryViewModel::setSelectedFolder,
			onSortOptionChange = libraryViewModel::setSortOption,
			onCreatePlaylist = libraryViewModel::createPlaylist,
			onDeletePlaylist = libraryViewModel::deletePlaylist,
			onRenamePlaylist = libraryViewModel::renamePlaylist,
			onAddSongToPlaylist = libraryViewModel::addSongToPlaylist,
			onRemoveSongFromPlaylist = libraryViewModel::removeSongFromPlaylist,
			onToggleFavorite = libraryViewModel::toggleFavorite,
			onToggleSongSelection = libraryViewModel::toggleSongSelection
		),
		playback = PlaybackEvents(
			onPlaySong = playbackViewModel::playSong,
			onPlayNext = playbackViewModel::playNext,
			onQuickResume = {
				playbackViewModel.resumeLastSession(libraryState.songs)
				libraryViewModel.refreshHomeSessionData()
			},
			onTogglePlayPause = playbackViewModel::togglePlayPause,
			onSkipNext = playbackViewModel::skipNext,
			onSkipPrevious = playbackViewModel::skipPrevious,
			onSeekTo = playbackViewModel::seekTo,
			onUpdateQueue = playbackViewModel::updateQueue,
			onToggleShuffle = playbackViewModel::toggleShuffle,
			onSetRepeatMode = playbackViewModel::setRepeatMode
		),
		settings = SettingsEvents(
			onSetTheme = onSetTheme,
			onSetHighResEnabled = { enabled ->
				audioSettingsState = audioSettingsState.copy(highResPlaybackEnabled = enabled)
				selectedPresetId = null
			},
			onSetShowAlbumArt = { enabled ->
				audioSettingsState = audioSettingsState.copy(showAlbumArt = enabled)
				selectedPresetId = null
			},
			onSetCrossfadeSeconds = { seconds ->
				audioSettingsState = audioSettingsState.copy(
					crossfadeSeconds = AudioSettingsState.clampCrossfadeSeconds(seconds)
				)
				selectedPresetId = null
			},
			onSelectPreset = ::applyPreset,
			onSaveCurrentAsPreset = {
				val id = "custom-$customPresetCounter"
				customPresets.add(
					SettingsPreset(
						id = id,
						title = "Custom $customPresetCounter",
						description = "Saved from current settings",
						audioSettings = audioSettingsState,
						themePreset = selectedTheme,
						isTemplate = false
					)
				)
				selectedPresetId = id
				customPresetCounter += 1
			},
			onResetToDefaults = {
				audioSettingsState = AudioSettingsState()
				onSetTheme(AppThemePreset.DEFAULT)
				selectedPresetId = null
			}
		),
		equalizer = EqualizerEvents(
			onToggleEqualizerEnabled = playbackViewModel::setEqualizerEnabled,
			onSetEqPreset = playbackViewModel::setPreset,
			onSetEqPreamp = playbackViewModel::setPreamp,
			onSetEqBandLevel = playbackViewModel::setBandLevel
		)
	)

	MusicAppContent(
		libraryState = libraryState,
		homeUiState = homeUiState,
		playbackState = playbackState,
		selectedSongIds = selectedSongIds,
		searchQuery = searchQuery,
		searchFilter = searchFilter,
		selectedFolder = selectedFolder,
		sortOption = sortOption,
		audioSettingsState = audioSettingsState,
		selectedTheme = selectedTheme,
		formatTime = playbackViewModel::formatTime,
		equalizerState = equalizerState,
		presets = allPresets,
		selectedPresetId = selectedPresetId,
		events = musicAppEvents,
		onSearchQueryChange = libraryViewModel::updateSearchQuery,
		onSearchFilterChange = libraryViewModel::setSearchFilter,
		onClearRecentSearches = libraryViewModel::clearRecentSearches,
		onRemoveRecentSearch = libraryViewModel::removeRecentSearch,
		onFolderSelected = libraryViewModel::setSelectedFolder,
		onSortOptionChange = libraryViewModel::setSortOption,
		onCreatePlaylist = libraryViewModel::createPlaylist,
		onDeletePlaylist = libraryViewModel::deletePlaylist,
		onRenamePlaylist = libraryViewModel::renamePlaylist,
		onAddSongToPlaylist = libraryViewModel::addSongToPlaylist,
		onRemoveSongFromPlaylist = libraryViewModel::removeSongFromPlaylist,
		onPlaySong = playbackViewModel::playSong,
		onPlayNext = playbackViewModel::playNext,
		onQuickResume = {
			playbackViewModel.resumeLastSession(libraryState.songs)
			libraryViewModel.refreshHomeSessionData()
		},
		onTogglePlayPause = playbackViewModel::togglePlayPause,
		onSkipNext = playbackViewModel::skipNext,
		onSkipPrevious = playbackViewModel::skipPrevious,
		onSeekTo = playbackViewModel::seekTo,
		onUpdateQueue = playbackViewModel::updateQueue,
		onToggleFavorite = libraryViewModel::toggleFavorite,
		onToggleSongSelection = libraryViewModel::toggleSongSelection,
		onToggleShuffle = playbackViewModel::toggleShuffle,
		onSetRepeatMode = playbackViewModel::setRepeatMode,
		onSetTheme = onSetTheme,
		onSetShowAlbumArt = { enabled ->
			audioSettingsState = audioSettingsState.copy(showAlbumArt = enabled)
			selectedPresetId = null
		},
		onSetHighResEnabled = { enabled ->
			audioSettingsState = audioSettingsState.copy(highResPlaybackEnabled = enabled)
			selectedPresetId = null
		},
		onSetCrossfadeSeconds = { seconds ->
			audioSettingsState = audioSettingsState.copy(crossfadeSeconds = seconds.coerceIn(0, 12))
			selectedPresetId = null
		},
		onToggleEqualizerEnabled = playbackViewModel::setEqualizerEnabled,
		onSetEqPreset = playbackViewModel::setPreset,
		onSetEqPreamp = playbackViewModel::setPreamp,
		onSetEqBandLevel = playbackViewModel::setBandLevel,
		onSelectPreset = ::applyPreset,
		onSaveCurrentAsPreset = {
			val id = "custom-$customPresetCounter"
			customPresets.add(
				SettingsPreset(
					id = id,
					title = "Custom $customPresetCounter",
					description = "Saved from current settings",
					audioSettings = audioSettingsState,
					themePreset = selectedTheme,
					isTemplate = false
				)
			)
			selectedPresetId = id
			customPresetCounter += 1
		},
		onResetToDefaults = {
			audioSettingsState = AudioSettingsState()
			onSetTheme(AppThemePreset.DEFAULT)
			selectedPresetId = null
		},
		isUserSignedIn = false
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
