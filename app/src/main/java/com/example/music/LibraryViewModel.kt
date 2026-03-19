package com.example.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(
    private val repository: MusicRepository,
    private val store: LibraryStore
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter.SONG)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _sortOption = MutableStateFlow(store.getLibrarySortOption())
    val sortOption: StateFlow<LibrarySortOption> = _sortOption.asStateFlow()

    init {
        refreshLibrary()
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val result = withContext(Dispatchers.IO) {
                repository.loadSongs()
            }

            _state.update {
                it.copy(
                    songs = result,
                    favorites = store.getFavorites(),
                    playlists = store.getPlaylists(),
                    recents = store.getRecent(),
                    playCounts = store.getPlayCounts(),
                    recentSearches = store.getRecentSearches(),
                    isLoading = false
                )
            }
        }
    }

    fun getFilteredSongs(): List<UiSong> {
        val query = _searchQuery.value.lowercase().trim()
        val filter = _searchFilter.value
        val state = _state.value

        val filtered = state.songs.filter { song ->
            when (filter) {
                SearchFilter.SONG -> song.title.contains(query, ignoreCase = true)
                SearchFilter.ARTIST -> song.artist.contains(query, ignoreCase = true)
                SearchFilter.ALBUM -> song.album.contains(query, ignoreCase = true)
            }
        }

        val searched = if (query.isBlank()) filtered else filtered.filter {
            it.title.contains(query, true) ||
                    it.artist.contains(query, true) ||
                    it.album.contains(query, true)
        }

        return searched.map {
            UiSong(
                song = it,
                isFavorite = state.favorites.contains(it.id),
                playCount = state.playCounts[it.id] ?: 0
            )
        }
    }

    fun recentlyAddedSongs(limit: Int = 12): List<Song> =
        _state.value.songs
            .sortedByDescending { it.dateAddedSec }
            .take(limit)

    fun mostPlayedSongs(limit: Int = 20): List<Song> =
        _state.value.songs
            .sortedByDescending { _state.value.playCounts[it.id] ?: 0 }
            .take(limit)

    fun songsForPlaylist(playlistName: String): List<Song> {
        val playlist = _state.value.playlists.firstOrNull { it.name == playlistName }
            ?: return emptyList()

        return playlist.songIds.mapNotNull { id ->
            _state.value.songs.firstOrNull { it.id == id }
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFavorites = store.toggleFavorite(songId)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(favorites = newFavorites) }
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            store.createPlaylist(name)
            val playlists = store.getPlaylists()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun addSongToPlaylist(playlistName: String, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            store.addSongToPlaylist(playlistName, songId)
            val playlists = store.getPlaylists()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun movePlaylist(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            store.reorderPlaylist(fromIndex, toIndex)
            val playlists = store.getPlaylists()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun toggleSongSelection(songId: Long) {
        _selectedSongIds.update { current ->
            current.toMutableSet().apply {
                if (!add(songId)) remove(songId)
            }
        }
    }

    fun removeSongFromPlaylist(playlistName: String, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            store.removeSongFromPlaylist(playlistName, songId)
            val playlists = store.getPlaylists()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun deletePlaylist(playlistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            store.deletePlaylist(playlistName)
            val playlists = store.getPlaylists()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun renamePlaylist(oldName: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            store.renamePlaylist(oldName, newName)
            val playlists = store.getPlaylists()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    fun addSelectionToPlaylist(playlistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedSongIds.value.forEach { songId ->
                store.addSongToPlaylist(playlistName, songId)
            }
            val playlists = store.getPlaylists()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(playlists = playlists) }
                clearSelection()
            }
        }
    }

    fun markSelectionFavorite() {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedSongIds.value.forEach { songId ->
                store.toggleFavorite(songId)
            }
            val favorites = store.getFavorites()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(favorites = favorites) }
                clearSelection()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            saveRecentSearch(query)
        }
    }

    fun setSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun setSelectedFolder(folder: String?) {
        _selectedFolder.value = folder
    }

    fun setSortOption(option: LibrarySortOption) {
        _sortOption.value = option
        viewModelScope.launch(Dispatchers.IO) {
            store.setLibrarySortOption(option)
        }
    }

    private fun saveRecentSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            store.saveRecentSearch(query)
            val searches = store.getRecentSearches()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(recentSearches = searches) }
            }
        }
    }
}