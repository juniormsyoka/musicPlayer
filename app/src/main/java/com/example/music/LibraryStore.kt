package com.example.music

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class LibraryStore(context: Context) {

    private val prefs = context.getSharedPreferences("music_store", Context.MODE_PRIVATE)

    fun getFavorites(): Set<Long> {
        return prefs.getStringSet(PrefKeys.FAVORITES, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    fun toggleFavorite(songId: Long): Set<Long> {
        val next = getFavorites().toMutableSet()
        if (!next.add(songId)) {
            next.remove(songId)
        }
        prefs.edit {
            putStringSet(PrefKeys.FAVORITES, next.map { it.toString() }.toSet())
        }
        return next
    }

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

    fun getPlaylists(): List<Playlist> {
        val raw = prefs.getString(PrefKeys.PLAYLISTS, "[]") ?: "[]"
        return runCatching { decodePlaylists(raw) }.getOrElse { emptyList() }
    }

    fun createPlaylist(name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return

        val existing = getPlaylists().toMutableList()
        if (existing.any { it.name.equals(normalized, ignoreCase = true) }) return

        existing += Playlist(normalized, emptyList())
        savePlaylists(existing)
    }

    fun addSongToPlaylist(playlistName: String, songId: Long) {
        val updated = getPlaylists().map { playlist ->
            if (!playlist.name.equals(playlistName, ignoreCase = true)) {
                playlist
            } else {
                val nextSongIds = playlist.songIds.toMutableList()
                if (songId !in nextSongIds) {
                    nextSongIds += songId
                }
                playlist.copy(songIds = nextSongIds)
            }
        }
        savePlaylists(updated)
    }

    fun removeSongFromPlaylist(playlistName: String, songId: Long) {
        val updated = getPlaylists().map { playlist ->
            if (!playlist.name.equals(playlistName, ignoreCase = true)) {
                playlist
            } else {
                playlist.copy(songIds = playlist.songIds.filterNot { it == songId })
            }
        }
        savePlaylists(updated)
    }

    fun deletePlaylist(playlistName: String) {
        val updated = getPlaylists().filterNot { it.name.equals(playlistName, ignoreCase = true) }
        savePlaylists(updated)
    }

    fun renamePlaylist(oldName: String, newName: String) {
        val normalized = newName.trim()
        if (normalized.isBlank()) return

        val current = getPlaylists()
        if (current.any { it.name.equals(normalized, ignoreCase = true) && !it.name.equals(oldName, ignoreCase = true) }) {
            return
        }

        val updated = current.map { playlist ->
            if (playlist.name.equals(oldName, ignoreCase = true)) {
                playlist.copy(name = normalized)
            } else {
                playlist
            }
        }
        savePlaylists(updated)
    }

    fun reorderPlaylist(fromIndex: Int, toIndex: Int) {
        val current = getPlaylists().toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        savePlaylists(current)
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

    fun getLibrarySortOption(): LibrarySortOption {
        val raw = prefs.getString(PrefKeys.LIBRARY_SORT, LibrarySortOption.TITLE_ASC.name)
        return LibrarySortOption.entries.firstOrNull { it.name == raw } ?: LibrarySortOption.TITLE_ASC
    }

    fun setLibrarySortOption(option: LibrarySortOption) {
        prefs.edit {
            putString(PrefKeys.LIBRARY_SORT, option.name)
        }
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        prefs.edit {
            putString(PrefKeys.PLAYLISTS, encodePlaylists(playlists))
        }
    }

    private fun savePlayCounts(playCounts: Map<Long, Int>) {
        prefs.edit {
            putString(PrefKeys.PLAY_COUNTS, encodePlayCounts(playCounts))
        }
    }

    private fun encodeLongList(values: List<Long>): String {
        return JSONArray().apply { values.forEach { put(it) } }.toString()
    }

    private fun decodeLongList(raw: String): List<Long> {
        val array = JSONArray(raw)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                add(array.getLong(index))
            }
        }
    }

    private fun encodeStringList(values: List<String>): String {
        return JSONArray().apply { values.forEach { put(it) } }.toString()
    }

    private fun decodeStringList(raw: String): List<String> {
        val array = JSONArray(raw)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                add(array.getString(index))
            }
        }
    }

    private fun encodePlaylists(playlists: List<Playlist>): String {
        return JSONArray().apply {
            playlists.forEach { playlist ->
                put(
                    JSONObject()
                        .put("name", playlist.name)
                        .put("songIds", JSONArray().apply { playlist.songIds.forEach { put(it) } })
                )
            }
        }.toString()
    }

    private fun decodePlaylists(raw: String): List<Playlist> {
        val array = JSONArray(raw)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val songIdsJson = item.optJSONArray("songIds") ?: JSONArray()
                val songIds = buildList(songIdsJson.length()) {
                    for (songIndex in 0 until songIdsJson.length()) {
                        add(songIdsJson.getLong(songIndex))
                    }
                }
                add(Playlist(name = item.optString("name", ""), songIds = songIds))
            }
        }
    }

    private fun encodePlayCounts(playCounts: Map<Long, Int>): String {
        return JSONObject().apply {
            playCounts.forEach { (songId, count) -> put(songId.toString(), count) }
        }.toString()
    }

    private fun decodePlayCounts(raw: String): Map<Long, Int> {
        val jsonObject = JSONObject(raw)
        val result = linkedMapOf<Long, Int>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            key.toLongOrNull()?.let { songId ->
                result[songId] = jsonObject.optInt(key, 0)
            }
        }
        return result
    }
}