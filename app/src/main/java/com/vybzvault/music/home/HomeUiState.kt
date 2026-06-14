package com.vybzvault.music.home

import androidx.compose.runtime.Stable
import com.vybzvault.music.Song

@Stable
data class HomeUiState(
    val isLoading: Boolean = false,
    val isEmptyLibrary: Boolean = false,
    val greeting: String = "",
    val contextualLine: String = "",
    val streak: HomeStreakUiModel = HomeStreakUiModel(),
    val weeklyDiscoveryCount: Int = 0,
    val resumeSession: ResumeSession? = null,
    val selectionCount: Int = 0,
    val selectedSongIds: Set<Long> = emptySet(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val sections: List<HomeSectionUiModel> = emptyList()
)

@Stable
data class HomeStreakUiModel(
    val currentDays: Int = 0,
    val bestDays: Int = 0,
    val activeDaysThisWeek: Int = 0,
    val nextMilestoneDays: Int = 3
)

@Stable
data class ResumeSession(
    val lastPlayedSong: Song,
    val positionSeconds: Int,
    val queuePreview: List<Song>
)

@Stable
data class HomeSectionUiModel(
    val key: String,
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    val emptyMessage: String
)

@Stable
data class HomeTimeSnapshot(
    val hour24: Int,
    val isWeekend: Boolean,
    val dayKey: Int
)




