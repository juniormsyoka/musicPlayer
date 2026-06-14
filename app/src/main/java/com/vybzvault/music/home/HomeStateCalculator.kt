package com.vybzvault.music.home

import com.vybzvault.music.LibraryState
import com.vybzvault.music.ListeningStreakInsights
import com.vybzvault.music.SavedPlaybackState
import com.vybzvault.music.Song
import java.util.Calendar

private data class TimeBasedRecommendation(
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    val emptyMessage: String
)

private data class HomeSectionCandidate(
    val key: String,
    val title: String,
    val subtitleBuilder: (Int) -> String,
    val songs: List<Song>,
    val emptyMessage: String,
    val maxSongs: Int = 6,
    val allowDuplicatesAcrossSections: Boolean = false
)

class HomeStateCalculator {

    fun calculate(
        libraryState: LibraryState,
        selectedSongIds: Set<Long>,
        timeSnapshot: HomeTimeSnapshot,
        savedPlaybackState: SavedPlaybackState?,
        streakInsights: ListeningStreakInsights
    ): HomeUiState {
        if (libraryState.isLoading && libraryState.songs.isEmpty()) {
            return HomeUiState(isLoading = true)
        }

        if (libraryState.songs.isEmpty()) {
            return HomeUiState(
                isEmptyLibrary = true,
                streak = streakInsights.toUiModel(),
                selectionCount = selectedSongIds.size,
                selectedSongIds = selectedSongIds
            )
        }

        val songs = libraryState.songs
        val playCounts = libraryState.playCounts
        val favorites = libraryState.favorites
        val songsById = songs.associateBy { it.id }
        val songsWithCount = songs.map { it to (playCounts[it.id] ?: 0) }

        val recentSongs = songs.sortedByDescending { it.dateAddedSec }.take(6)
        val continueListeningSongs = libraryState.recents
            .asSequence()
            .mapNotNull { songsById[it] }
            .distinctBy { it.id }
            .take(6)
            .toList()

        val mostPlayedSongs = songsWithCount
            .filter { (_, count) -> count > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(6)

        val favoriteSongs = songs
            .filter { favorites.contains(it.id) }
            .take(6)

        val hiddenGems = songsWithCount
            .filter { (_, count) -> count in 1..2 }
            .sortedByDescending { it.first.dateAddedSec }
            .map { it.first }
            .take(6)

        val mixOfTheDaySongs = buildMixOfTheDay(
            songs = songs,
            favorites = favorites,
            playCounts = playCounts,
            dayKey = timeSnapshot.dayKey
        )

        val recommendation = buildTimeBasedRecommendation(
            songsWithCount = songsWithCount,
            favorites = favorites,
            nowHour = timeSnapshot.hour24,
            isWeekend = timeSnapshot.isWeekend
        )

        val totalPlays = playCounts.values.sum()
        val weeklyDiscoveryCount = countWeeklyDiscoveries(
            firstPlayedDayKeys = libraryState.firstPlayedDayKeys,
            currentDayKey = timeSnapshot.dayKey
        )
        val greeting = greetingForHour(timeSnapshot.hour24)
        val contextualLine = buildContextualLine(
            songCount = songs.size,
            favoriteCount = favorites.size,
            totalPlays = totalPlays,
            weeklyDiscoveryCount = weeklyDiscoveryCount,
            isWeekend = timeSnapshot.isWeekend
        )

        val becauseYouPlayedSection = buildBecauseYouPlayedSection(
            songsById = songsById,
            transitions = libraryState.songTransitions,
            lastPlayedSongId = savedPlaybackState?.lastPlayedSongId
        )

        val sections = buildHomeSections(
            allSongs = songs,
            becauseYouPlayedSection = becauseYouPlayedSection,
            recommendation = recommendation,
            mixOfTheDaySongs = mixOfTheDaySongs,
            continueListeningSongs = continueListeningSongs,
            recentSongs = recentSongs,
            mostPlayedSongs = mostPlayedSongs,
            hiddenGems = hiddenGems,
            favoriteSongs = favoriteSongs,
            timeSnapshot = timeSnapshot
        )

        val resumeSession = savedPlaybackState?.toResumeSession(songsById)

        return HomeUiState(
            greeting = greeting,
            contextualLine = contextualLine,
            streak = streakInsights.toUiModel(),
            weeklyDiscoveryCount = weeklyDiscoveryCount,
            resumeSession = resumeSession,
            selectionCount = selectedSongIds.size,
            selectedSongIds = selectedSongIds,
            favoriteSongIds = favorites,
            sections = sections
        )
    }

    private fun SavedPlaybackState.toResumeSession(songsById: Map<Long, Song>): ResumeSession? {
        val lastSong = songsById[lastPlayedSongId] ?: return null
        val previewSongs = queueSongIds
            .asSequence()
            .mapNotNull { songsById[it] }
            .filterNot { it.id == lastPlayedSongId }
            .distinctBy { it.id }
            .take(4)
            .toList()

        return ResumeSession(
            lastPlayedSong = lastSong,
            positionSeconds = (playbackPositionMs / 1_000L).toInt().coerceAtLeast(0),
            queuePreview = if (previewSongs.isEmpty()) listOf(lastSong) else previewSongs
        )
    }

    private fun buildMixOfTheDay(
        songs: List<Song>,
        favorites: Set<Long>,
        playCounts: Map<Long, Int>,
        dayKey: Int
    ): List<Song> {
        if (songs.isEmpty()) return emptyList()

        val targetMixSize = 30
        val selected = mutableListOf<Song>()
        val usedIds = mutableSetOf<Long>()

        val heavyRotationTracks = songs
            .filter { (playCounts[it.id] ?: 0) >= 5 }
            .sortedByWithDaySeed(dayKey)

        // 70% Comfort Zone: Favorites & Heavy Rotations
        val comfortPool = songs.filter { favorites.contains(it.id) || it in heavyRotationTracks }
            .sortedByWithDaySeed(dayKey)

        val comfortCount = (targetMixSize * 0.7).toInt()
        for (song in comfortPool) {
            if (selected.size >= comfortCount) break
            if (usedIds.add(song.id)) selected.add(song)
        }

        // 20% Nostalgia / Forgotten Gems
        val recentIds = songs.sortedByDescending { it.dateAddedSec }.take(15).map { it.id }.toSet()
        val nostalgiaPool = songs
            .filter { !favorites.contains(it.id) && (playCounts[it.id] ?: 0) in 2..5 && it.id !in recentIds }
            .sortedByWithDaySeed(dayKey + 1)

        val nostalgiaCount = (targetMixSize * 0.2).toInt()
        for (song in nostalgiaPool) {
            if (selected.size >= (comfortCount + nostalgiaCount)) break
            if (usedIds.add(song.id)) selected.add(song)
        }

        // 10% Smart Discovery: Unplayed paths
        val discoveryPool = songs
            .filter { (playCounts[it.id] ?: 0) == 0 }
            .sortedByWithDaySeed(dayKey + 2)

        while (selected.size < targetMixSize) {
            val nextDiscovery = discoveryPool.firstOrNull { it.id !in usedIds }
            if (nextDiscovery != null) {
                usedIds.add(nextDiscovery.id)
                selected.add(nextDiscovery)
            } else break
        }

        // Dynamic Fallback protection
        if (selected.size < targetMixSize) {
            val genericFallback = songs.sortedByWithDaySeed(dayKey + 3)
            for (song in genericFallback) {
                if (selected.size >= targetMixSize) break
                if (usedIds.add(song.id)) selected.add(song)
            }
        }

        return selected.sortedWith(
            compareBy<Song> { it.artist.lowercase() }
                .thenBy { dailyOrderKey(it.id, dayKey) }
        )
    }

    private fun List<Song>.sortedByWithDaySeed(dayKey: Int): List<Song> {
        return this.sortedBy { dailyOrderKey(it.id, dayKey) }
    }

    private fun dailyOrderKey(songId: Long, dayKey: Int): Long {
        val mixed = songId xor dayKey.toLong()
        return mixed * 1103515245L + 12345L
    }

    private fun buildContextualLine(
        songCount: Int,
        favoriteCount: Int,
        totalPlays: Int,
        weeklyDiscoveryCount: Int,
        isWeekend: Boolean
    ): String {
        val discoveryLabel = if (weeklyDiscoveryCount == 1) "song" else "songs"
        val modeHint = if (isWeekend) " Weekend mode is on." else ""
        return "You've discovered $weeklyDiscoveryCount new $discoveryLabel this week. Library: $songCount songs, $favoriteCount favorites, $totalPlays plays.$modeHint"
    }

    private fun countWeeklyDiscoveries(firstPlayedDayKeys: Map<Long, Int>, currentDayKey: Int): Int {
        if (firstPlayedDayKeys.isEmpty()) return 0
        val weekStart = startOfWeekDayKey(currentDayKey)
        return firstPlayedDayKeys.values.count { it in weekStart..currentDayKey }
    }

    private fun startOfWeekDayKey(dayKey: Int): Int {
        val calendar = calendarFromDayKey(dayKey)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val delta = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -delta)
        return dayKeyFromCalendar(calendar)
    }

    private fun calendarFromDayKey(dayKey: Int): Calendar {
        val year = dayKey / 10_000
        val month = (dayKey / 100) % 100
        val day = dayKey % 100
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceAtLeast(0))
            set(Calendar.DAY_OF_MONTH, day.coerceAtLeast(1))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun dayKeyFromCalendar(calendar: Calendar): Int {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return year * 10_000 + month * 100 + day
    }

    private fun buildBecauseYouPlayedSection(
        songsById: Map<Long, Song>,
        transitions: Map<Long, Map<Long, Int>>,
        lastPlayedSongId: Long?
    ): TimeBasedRecommendation? {
        val anchorId = lastPlayedSongId ?: return null
        val anchorSong = songsById[anchorId] ?: return null
        val outgoing = transitions[anchorId].orEmpty()
        if (outgoing.isEmpty()) return null

        val picks = outgoing.entries
            .sortedByDescending { it.value }
            .mapNotNull { (songId, _) -> songsById[songId] }
            .filterNot { it.id == anchorId }
            .take(6)

        if (picks.isEmpty()) return null

        return TimeBasedRecommendation(
            title = "Because You Played ${anchorSong.title}",
            subtitle = "Songs you usually play next",
            songs = picks,
            emptyMessage = "Play more songs in a row to unlock contextual picks."
        )
    }

    private fun greetingForHour(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    private fun buildTimeBasedRecommendation(
        songsWithCount: List<Pair<Song, Int>>,
        favorites: Set<Long>,
        nowHour: Int,
        isWeekend: Boolean
    ): TimeBasedRecommendation {
        return when (nowHour) {
            in 5..11 -> {
                val picks = songsWithCount
                    .sortedWith(compareBy<Pair<Song, Int>> { it.second }.thenByDescending { it.first.dateAddedSec })
                    .map { it.first }
                    .take(6)
                TimeBasedRecommendation(
                    title = "Morning Discovery",
                    subtitle = if (isWeekend) "Easy picks for a slow weekend start" else "Fresh picks to start your day",
                    songs = picks,
                    emptyMessage = "New recommendations appear after you add more songs."
                )
            }

            in 12..16 -> {
                val mediumReplayPicks = songsWithCount
                    .filter { (_, count) -> count in 1..8 }
                    .sortedByDescending { it.second }
                    .map { it.first }

                val picks = if (mediumReplayPicks.isNotEmpty()) {
                    mediumReplayPicks.take(6)
                } else {
                    songsWithCount.sortedByDescending { it.second }.map { it.first }.take(6)
                }

                TimeBasedRecommendation(
                    title = "Afternoon Focus",
                    subtitle = if (isWeekend) "Laid-back tracks for your afternoon" else "Steady tracks with moderate replay counts",
                    songs = picks,
                    emptyMessage = "Play a few songs and we will tune this section."
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
                    subtitle = if (isWeekend) "Weekend wind-down with your favorites" else "Wind down with your best-loved tracks",
                    songs = picks,
                    emptyMessage = "Mark favorites to improve evening picks."
                )
            }
        }
    }

    private fun buildHomeSections(
        allSongs: List<Song>,
        becauseYouPlayedSection: TimeBasedRecommendation?,
        recommendation: TimeBasedRecommendation,
        mixOfTheDaySongs: List<Song>,
        continueListeningSongs: List<Song>,
        recentSongs: List<Song>,
        mostPlayedSongs: List<Song>,
        hiddenGems: List<Song>,
        favoriteSongs: List<Song>,
        timeSnapshot: HomeTimeSnapshot
    ): List<HomeSectionUiModel> {
        val sections = mutableListOf<HomeSectionUiModel>()
        val usedSongIds = mutableSetOf<Long>()

        // 1. DYNAMIC GENERATION: Build up to 3 Smart Micro-Mixes based on Top Artists
        val topArtists = mostPlayedSongs
            .map { it.artist.trim() }
            .filter { it.isNotEmpty() && !it.equals("<unknown>", ignoreCase = true) }
            .distinct()
            .take(3)

        if (topArtists.isNotEmpty()) {
            topArtists.forEachIndexed { index, artistName ->
                val artistTracksPool = (mostPlayedSongs + recentSongs + favoriteSongs + mixOfTheDaySongs)
                    .distinctBy { it.id }
                    .filter { it.artist.equals(artistName, ignoreCase = true) }
                    .sortedBy { dailyOrderKey(it.id, timeSnapshot.dayKey + index) }
                    .take(12)

                if (artistTracksPool.size >= 3) {
                    sections.add(
                        HomeSectionUiModel(
                            key = "artist-mix-${artistName.lowercase().replace(" ", "-")}",
                            title = "$artistName Mix",
                            subtitle = "Daily capsule of your favorite $artistName vibes",
                            songs = artistTracksPool,
                            emptyMessage = ""
                        )
                    )
                }
            }
        }

        // =========================================================================
        // 🛠️ FIX: primarySection is now properly defined BEFORE the candidates list
        // =========================================================================
        val primarySection = becauseYouPlayedSection ?: recommendation

        // 2. Map standard structures out cleanly
        val candidates = listOf(
            HomeSectionCandidate(
                key = if (becauseYouPlayedSection != null) "because-you-played" else "recommendation",
                title = primarySection.title,
                subtitleBuilder = { primarySection.subtitle },
                songs = primarySection.songs,
                emptyMessage = primarySection.emptyMessage
            ),
            HomeSectionCandidate(
                key = "recently-added",
                title = "Recently Added",
                subtitleBuilder = { count -> "$count songs" },
                songs = recentSongs,
                emptyMessage = "New songs will show up here.",
                allowDuplicatesAcrossSections = true
            ),
            HomeSectionCandidate(
                key = "favorites",
                title = "Favorites",
                subtitleBuilder = { count -> "$count songs" },
                songs = favoriteSongs,
                emptyMessage = "Tap the heart icon to add songs to favorites.",
                allowDuplicatesAcrossSections = true
            ),
            HomeSectionCandidate(
                key = "mix-of-the-day",
                title = "Mix of the Day",
                subtitleBuilder = { count -> "$count-song smart mix" },
                songs = mixOfTheDaySongs,
                emptyMessage = "Keep listening and this mix will adapt to your taste.",
                maxSongs = 10,
                allowDuplicatesAcrossSections = true
            ),
            HomeSectionCandidate(
                key = "continue-listening",
                title = "Continue Listening",
                subtitleBuilder = { count -> if (count > 0) "$count recent picks" else "Pick up where you left off" },
                songs = continueListeningSongs,
                emptyMessage = "Play some songs to build your continue listening row.",
                allowDuplicatesAcrossSections = true
            ),
            HomeSectionCandidate(
                key = "most-played",
                title = "Most Played",
                subtitleBuilder = { count -> if (count > 0) "$count songs" else "Your top songs will appear here" },
                songs = mostPlayedSongs,
                emptyMessage = "Start playing songs to see your most played tracks.",
                allowDuplicatesAcrossSections = true
            ),
            HomeSectionCandidate(
                key = "hidden-gems",
                title = "Hidden Gems",
                subtitleBuilder = { count -> if (count > 0) "$count lightly played songs" else "Rediscover lightly played tracks" },
                songs = hiddenGems,
                emptyMessage = "Play a few songs once or twice to surface hidden gems."
            )
        )

        for (candidate in candidates) {
            val uniqueSongs = mutableListOf<Song>()
            for (song in candidate.songs) {
                if (!candidate.allowDuplicatesAcrossSections) {
                    if (song.id in usedSongIds) continue
                    usedSongIds.add(song.id)
                }
                uniqueSongs.add(song)
                if (uniqueSongs.size == candidate.maxSongs) break
            }

            if (uniqueSongs.isNotEmpty()) {
                sections.add(
                    HomeSectionUiModel(
                        key = candidate.key,
                        title = candidate.title,
                        subtitle = candidate.subtitleBuilder(uniqueSongs.size),
                        songs = uniqueSongs,
                        emptyMessage = candidate.emptyMessage
                    )
                )
            }
        }

        return sections
    }
    private fun ListeningStreakInsights.toUiModel(): HomeStreakUiModel {
        return HomeStreakUiModel(
            currentDays = currentStreakDays,
            bestDays = bestStreakDays,
            activeDaysThisWeek = activeDaysThisWeek,
            nextMilestoneDays = nextMilestoneDays
        )
    }
}