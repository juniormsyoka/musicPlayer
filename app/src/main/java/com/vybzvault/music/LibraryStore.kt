package com.vybzvault.music

import android.content.Context

class LibraryStore(context: Context) {

    private val favoritesStore = FavoritesStore(context)
    private val playlistStore = PlaylistStore(context)
    private val historyStore = LibraryHistoryStore(context)

    fun getFavorites(): Set<Long> = favoritesStore.getFavorites()

    fun toggleFavorite(songId: Long): Set<Long> = favoritesStore.toggleFavorite(songId)

    fun addRecent(songId: Long) = historyStore.addRecent(songId)

    fun getRecent(): List<Long> = historyStore.getRecent()

    fun getPlaylists(): List<Playlist> = playlistStore.getPlaylists()

    fun createPlaylist(name: String) = playlistStore.createPlaylist(name)

    fun addSongToPlaylist(playlistName: String, songId: Long) = playlistStore.addSongToPlaylist(playlistName, songId)

    fun removeSongFromPlaylist(playlistName: String, songId: Long) = playlistStore.removeSongFromPlaylist(playlistName, songId)

    fun deletePlaylist(playlistName: String) = playlistStore.deletePlaylist(playlistName)

    fun renamePlaylist(oldName: String, newName: String) = playlistStore.renamePlaylist(oldName, newName)

    fun reorderPlaylist(fromIndex: Int, toIndex: Int) = playlistStore.reorderPlaylist(fromIndex, toIndex)

    fun incrementPlayCount(songId: Long) = historyStore.incrementPlayCount(songId)

    fun getPlayCounts(): Map<Long, Int> = historyStore.getPlayCounts()

    fun saveRecentSearch(query: String) = historyStore.saveRecentSearch(query)

    fun getRecentSearches(): List<String> = historyStore.getRecentSearches()

    fun clearRecentSearches() = historyStore.clearRecentSearches()

    fun removeRecentSearch(query: String) = historyStore.removeRecentSearch(query)

    fun getLibrarySortOption(): LibrarySortOption = historyStore.getLibrarySortOption()

    fun setLibrarySortOption(option: LibrarySortOption) = historyStore.setLibrarySortOption(option)

    fun getSongTransitions(): Map<Long, Map<Long, Int>> = historyStore.getSongTransitions()

    fun getFirstPlayedDayKeys(): Map<Long, Int> = historyStore.getFirstPlayedDayKeys()

    fun recordPlayEvent(songId: Long, previousSongId: Long?, nowEpochMs: Long = System.currentTimeMillis()) {
        historyStore.recordPlayEvent(songId, previousSongId, nowEpochMs)
    }
}