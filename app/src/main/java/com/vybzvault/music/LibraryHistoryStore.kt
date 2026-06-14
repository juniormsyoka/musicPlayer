package com.vybzvault.music

import android.content.Context
import androidx.core.content.edit

class LibraryHistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("music_store", Context.MODE_PRIVATE)

    fun addRecent(songId: Long) {
        val current = getRecent().toMutableList()
        current.remove(songId)
        current.add(0, songId)
        val trimmed = current.take(PlayerConstants.MAX_RECENT_SONGS)
        prefs.edit {
            putString(PrefKeys.RECENTS, encodeLongList(trimmed))
        }
    }

    fun getRecent(): List<Long> {
        val raw = prefs.getString(PrefKeys.RECENTS, "[]") ?: "[]"
        return runCatching { decodeLongList(raw) }.getOrElse { emptyList() }
    }

    fun incrementPlayCount(songId: Long) {
        val counts = getPlayCounts().toMutableMap()
        counts[songId] = (counts[songId] ?: 0) + 1
        savePlayCounts(counts)
    }

    fun getPlayCounts(): Map<Long, Int> {
        val raw = prefs.getString(PrefKeys.PLAY_COUNTS, "{}") ?: "{}"
        return runCatching { decodePlayCounts(raw) }.getOrElse { emptyMap() }
    }

    fun saveRecentSearch(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return

        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(normalized, ignoreCase = true) }
        current.add(0, normalized)

        prefs.edit {
            putString(PrefKeys.RECENT_SEARCHES, encodeStringList(current.take(PlayerConstants.MAX_RECENT_SEARCHES)))
        }
    }

    fun getRecentSearches(): List<String> {
        val raw = prefs.getString(PrefKeys.RECENT_SEARCHES, "[]") ?: "[]"
        return runCatching { decodeStringList(raw) }.getOrElse { emptyList() }
    }

    fun clearRecentSearches() {
        prefs.edit {
            putString(PrefKeys.RECENT_SEARCHES, "[]")
        }
    }

    fun removeRecentSearch(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        val updated = getRecentSearches().filterNot { it.equals(normalized, ignoreCase = true) }
        prefs.edit {
            putString(PrefKeys.RECENT_SEARCHES, encodeStringList(updated))
        }
    }

    fun getLibrarySortOption(): LibrarySortOption {
        val raw = prefs.getString(PrefKeys.LIBRARY_SORT, LibrarySortOption.TITLE_ASC.name)
        return LibrarySortOption.entries.firstOrNull { it.name == raw } ?: LibrarySortOption.TITLE_ASC
    }

    fun setLibrarySortOption(option: LibrarySortOption) {
        prefs.edit {
            putString(PrefKeys.LIBRARY_SORT, option.name)
        }
    }

    fun getSongTransitions(): Map<Long, Map<Long, Int>> {
        val raw = prefs.getString(PrefKeys.SONG_TRANSITIONS, "{}") ?: "{}"
        return runCatching { decodeSongTransitions(raw) }.getOrElse { emptyMap() }
    }

    fun getFirstPlayedDayKeys(): Map<Long, Int> {
        val raw = prefs.getString(PrefKeys.FIRST_PLAYED_DAY_KEYS, "{}") ?: "{}"
        return runCatching { decodeIntMap(raw) }.getOrElse { emptyMap() }
    }

    fun recordPlayEvent(songId: Long, previousSongId: Long?, nowEpochMs: Long = System.currentTimeMillis()) {
        incrementPlayCount(songId)
        addRecent(songId)

        val firstPlayed = getFirstPlayedDayKeys().toMutableMap()
        if (songId !in firstPlayed) {
            firstPlayed[songId] = dayKey(nowEpochMs)
            prefs.edit {
                putString(PrefKeys.FIRST_PLAYED_DAY_KEYS, encodeIntMap(firstPlayed))
            }
        }

        val fromSongId = previousSongId ?: return
        if (fromSongId == songId) return

        val transitions = getSongTransitions().mapValues { (_, value) -> value.toMutableMap() }.toMutableMap()
        val outgoing = (transitions[fromSongId] ?: emptyMap()).toMutableMap()
        outgoing[songId] = (outgoing[songId] ?: 0) + 1
        transitions[fromSongId] = outgoing

        prefs.edit {
            putString(PrefKeys.SONG_TRANSITIONS, encodeSongTransitions(transitions))
        }
    }

    private fun savePlayCounts(playCounts: Map<Long, Int>) {
        prefs.edit {
            putString(PrefKeys.PLAY_COUNTS, encodePlayCounts(playCounts))
        }
    }
}

