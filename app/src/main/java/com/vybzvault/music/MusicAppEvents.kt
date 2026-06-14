package com.vybzvault.music

import com.vybzvault.music.equalizer.EqPreset
import com.vybzvault.music.ui.theme.AppThemePreset

sealed interface MusicAppEvents {
    val library: LibraryEvents
    val playback: PlaybackEvents
    val settings: SettingsEvents
    val equalizer: EqualizerEvents

    data class Default(
        override val library: LibraryEvents,
        override val playback: PlaybackEvents,
        override val settings: SettingsEvents,
        override val equalizer: EqualizerEvents
    ) : MusicAppEvents
}

data class LibraryEvents(
    val onSearchQueryChange: (String) -> Unit,
    val onSearchFilterChange: (SearchFilter) -> Unit,
    val onClearRecentSearches: () -> Unit,
    val onRemoveRecentSearch: (String) -> Unit,
    val onFolderSelected: (String?) -> Unit,
    val onSortOptionChange: (LibrarySortOption) -> Unit,
    val onCreatePlaylist: (String) -> Unit,
    val onDeletePlaylist: (String) -> Unit,
    val onRenamePlaylist: (String, String) -> Unit,
    val onAddSongToPlaylist: (String, Long) -> Unit,
    val onRemoveSongFromPlaylist: (String, Long) -> Unit,
    val onToggleFavorite: (Long) -> Unit,
    val onToggleSongSelection: (Long) -> Unit
)

data class PlaybackEvents(
    val onPlaySong: (Song, List<Song>) -> Unit,
    val onPlayNext: (Song) -> Unit,
    val onQuickResume: () -> Unit,
    val onTogglePlayPause: () -> Unit,
    val onSkipNext: () -> Unit,
    val onSkipPrevious: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onUpdateQueue: (List<Song>) -> Unit,
    val onToggleShuffle: () -> Unit,
    val onSetRepeatMode: (RepeatMode) -> Unit
)

data class SettingsEvents(
    val onSetTheme: (AppThemePreset) -> Unit,
    val onSetHighResEnabled: (Boolean) -> Unit,
    val onSetShowAlbumArt: (Boolean) -> Unit,
    val onSetCrossfadeSeconds: (Int) -> Unit,
    val onSelectPreset: (String) -> Unit,
    val onSaveCurrentAsPreset: () -> Unit,
    val onResetToDefaults: () -> Unit
)

data class EqualizerEvents(
    val onToggleEqualizerEnabled: (Boolean) -> Unit,
    val onSetEqPreset: (EqPreset) -> Unit,
    val onSetEqPreamp: (Float) -> Unit,
    val onSetEqBandLevel: (Int, Int) -> Unit
)