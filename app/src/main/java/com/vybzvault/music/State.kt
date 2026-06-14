package com.vybzvault.music

import androidx.compose.runtime.Stable
import com.vybzvault.music.equalizer.EqPreset
import com.vybzvault.music.equalizer.EqualizerDefaults

@Stable
sealed class PlayerState {
    object Idle : PlayerState()
    object Preparing : PlayerState()
    data class Ready(val song: Song, val duration: Long) : PlayerState()
    data class Playing(val song: Song, val position: Long, val duration: Long) : PlayerState()
    data class Paused(val song: Song, val position: Long, val duration: Long) : PlayerState()
    data class Error(val message: String, val song: Song? = null) : PlayerState()
}

@Stable
sealed class Result<out T> {
    object Loading : Result<Nothing>()
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

@Stable
data class PlaybackState(
    val playerState: PlayerState = PlayerState.Idle,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val shuffleEnabled: Boolean = false,
    val shuffledIndices: List<Int> = emptyList()
)

@Stable
data class LibraryState(
    val songs: List<Song> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val playlists: List<Playlist> = emptyList(),
    val recents: List<Long> = emptyList(),
    val playCounts: Map<Long, Int> = emptyMap(),
    val songTransitions: Map<Long, Map<Long, Int>> = emptyMap(),
    val firstPlayedDayKeys: Map<Long, Int> = emptyMap(),
    val recentSearches: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@Stable
data class UiSong(
    val song: Song,
    val isFavorite: Boolean,
    val playCount: Int = 0
)

@Stable
data class MusicUiState(
    val library: LibraryState = LibraryState(),
    val playback: PlaybackState = PlaybackState(),
    val selectedScreen: AppScreen = AppScreen.HOME,
    val selectedSongIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val searchFilter: SearchFilter = SearchFilter.SONG,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val equalizer: EqualizerState = EqualizerState()
)

@Stable
data class SleepTimerState(
    val targetTimeMs: Long? = null,
    val remainingMs: Long = 0L,
    val isActive: Boolean = false
)

@Stable
data class EqualizerState(
    val levels: List<Int> = emptyList(),
    val range: Pair<Int, Int> = -1500 to 1500,
    val isEnabled: Boolean = false,
    val audioSessionId: Int = 0,
    val selectedPreset: EqPreset = EqPreset.CUSTOM,
    val preampDb: Float = 0f,
    val supportedBandCount: Int = 0,
    val bandFrequenciesHz: List<Int> = EqualizerDefaults.BAND_FREQUENCIES_HZ,
    val error: String? = null
)

enum class AppScreen { HOME, PLAYLISTS, SEARCH, SETTINGS }
enum class RepeatMode { OFF, ALL, ONE }
enum class SearchFilter { SONG, ARTIST, ALBUM }
enum class LibrarySortOption { TITLE_ASC, TITLE_DESC, ARTIST, RECENTLY_ADDED, DURATION }
