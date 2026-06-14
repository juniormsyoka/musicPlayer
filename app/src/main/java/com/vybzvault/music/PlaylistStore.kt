package com.vybzvault.music

import android.content.Context
import androidx.core.content.edit

class PlaylistStore(context: Context) {

    private val prefs = context.getSharedPreferences("music_store", Context.MODE_PRIVATE)

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

    private fun savePlaylists(playlists: List<Playlist>) {
        prefs.edit {
            putString(PrefKeys.PLAYLISTS, encodePlaylists(playlists))
        }
    }
}

