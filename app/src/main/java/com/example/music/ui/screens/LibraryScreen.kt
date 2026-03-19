package com.example.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.music.LibraryState
import com.example.music.LibrarySortOption
import com.example.music.Playlist
import com.example.music.Song
import com.example.music.ui.components.AlbumArt

@Composable
fun LibraryScreen(
    libraryState: LibraryState,
    selectedFolder: String?,
    sortOption: LibrarySortOption,
    selectedSongIds: Set<Long>,
    onFolderSelected: (String?) -> Unit,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onAddSongToPlaylist: (String, Long) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylistName by remember { mutableStateOf<String?>(null) }
    var playlistToRename by remember { mutableStateOf<String?>(null) }
    var playlistToDelete by remember { mutableStateOf<String?>(null) }
    var playlistForAddSongs by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with Folders and Sort
        LibraryHeader(
            libraryState = libraryState,
            selectedFolder = selectedFolder,
            sortOption = sortOption,
            onFolderSelected = onFolderSelected,
            onSortOptionChange = onSortOptionChange,
            onCreatePlaylist = { showCreatePlaylistDialog = true }
        )

        // Library Content
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Playlists Section
            if (selectedFolder == null) {
                item {
                    LibrarySection("Playlists", "${libraryState.playlists.size} playlists")
                }
                if (libraryState.playlists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No playlists yet. Create one to get started!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    item {
                        if (selectedPlaylistName != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(onClick = { selectedPlaylistName = null }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("All playlists")
                                }
                                OutlinedButton(onClick = { playlistForAddSongs = selectedPlaylistName }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add songs")
                                }
                            }
                        }
                    }

                    if (selectedPlaylistName == null) {
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    libraryState.playlists,
                                    key = { it.name }
                                ) { playlist ->
                                    PlaylistItem(
                                        modifier = Modifier.width(260.dp),
                                        playlist = playlist,
                                        songCount = libraryState.songs.count { it.id in playlist.songIds },
                                        onClick = { selectedPlaylistName = playlist.name },
                                        onRename = { playlistToRename = playlist.name },
                                        onDelete = { playlistToDelete = playlist.name }
                                    )
                                }
                            }
                        }
                    } 
                    else {
                        val playlist = libraryState.playlists.firstOrNull { it.name == selectedPlaylistName }
                        val playlistSongs = playlist?.songIds?.mapNotNull { id ->
                            libraryState.songs.firstOrNull { it.id == id }
                        }.orEmpty()

                        item {
                            LibrarySection(selectedPlaylistName ?: "Playlist", "${playlistSongs.size} songs")
                        }
                        if (playlistSongs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "This playlist is empty. Tap Add songs.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(playlistSongs, key = { "playlist-${selectedPlaylistName}-${it.id}" }) { song ->
                                LibrarySongItem(
                                    song = song,
                                    isFavorite = libraryState.favorites.contains(song.id),
                                    isSelected = selectedSongIds.contains(song.id),
                                    onPlay = { onPlaySong(song, playlistSongs) },
                                    onToggleFavorite = { onToggleFavorite(song.id) },
                                    onToggleSelection = { onToggleSongSelection(song.id) },
                                    trailingAction = {
                                        IconButton(onClick = {
                                            selectedPlaylistName?.let { name ->
                                                onRemoveSongFromPlaylist(name, song.id)
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.RemoveCircleOutline,
                                                contentDescription = "Remove from playlist",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Favorites Section
            val favoriteSongs = libraryState.songs.filter {
                libraryState.favorites.contains(it.id) &&
                        (selectedFolder == null || it.folder == selectedFolder)
            }
          /*  if (favoriteSongs.isNotEmpty()) {
                item {
                    LibrarySection("Favorites", "${favoriteSongs.size} songs")
                }
                items(favoriteSongs, key = { "favorites-${it.id}" }) { song ->
                    LibrarySongItem(
                        song = song,
                        isFavorite = true,
                        isSelected = selectedSongIds.contains(song.id),
                        onPlay = { onPlaySong(song, favoriteSongs) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onToggleSelection = { onToggleSongSelection(song.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            } */

            // All Songs Section (filtered by folder)
            val displayedSongs = if (selectedFolder != null) {
                libraryState.songs.filter { it.folder == selectedFolder }
            } else {
                libraryState.songs
            }

            val sortedSongs = when (sortOption) {
                LibrarySortOption.TITLE_ASC -> displayedSongs.sortedBy { it.title }
                LibrarySortOption.TITLE_DESC -> displayedSongs.sortedByDescending { it.title }
                LibrarySortOption.ARTIST -> displayedSongs.sortedWith(
                    compareBy<Song> { it.artist }.thenBy { it.title }
                )
                LibrarySortOption.RECENTLY_ADDED -> displayedSongs.sortedByDescending { it.dateAddedSec }
                LibrarySortOption.DURATION -> displayedSongs.sortedByDescending { it.durationMs }
            }

            item {
                LibrarySection("All Songs", "${sortedSongs.size} songs")
            }

            if (sortedSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No songs in this folder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sortedSongs, key = { "all-${it.id}" }) { song ->
                    LibrarySongItem(
                        song = song,
                        isFavorite = libraryState.favorites.contains(song.id),
                        isSelected = selectedSongIds.contains(song.id),
                        onPlay = { onPlaySong(song, sortedSongs) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onToggleSelection = { onToggleSongSelection(song.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    playlistToRename?.let { oldName ->
        RenamePlaylistDialog(
            currentName = oldName,
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                onRenamePlaylist(oldName, newName)
                if (selectedPlaylistName == oldName) selectedPlaylistName = newName
                playlistToRename = null
            }
        )
    }

    playlistToDelete?.let { name ->
        ConfirmDeletePlaylistDialog(
            playlistName = name,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                onDeletePlaylist(name)
                if (selectedPlaylistName == name) selectedPlaylistName = null
                playlistToDelete = null
            }
        )
    }

    playlistForAddSongs?.let { playlistName ->
        AddSongsToPlaylistDialog(
            playlistName = playlistName,
            songs = libraryState.songs,
            existingSongIds = libraryState.playlists.firstOrNull { it.name == playlistName }?.songIds.orEmpty().toSet(),
            onDismiss = { playlistForAddSongs = null },
            onAdd = { songIds ->
                songIds.forEach { songId -> onAddSongToPlaylist(playlistName, songId) }
                playlistForAddSongs = null
            }
        )
    }
}

@Composable
private fun LibraryHeader(
    libraryState: LibraryState,
    selectedFolder: String?,
    sortOption: LibrarySortOption,
    onFolderSelected: (String?) -> Unit,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    val folderOptions = remember(libraryState.songs) {
        libraryState.songs.map { it.folder }.distinct().sorted()
    }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Title
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Folder Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFolder == null,
                onClick = { onFolderSelected(null) },
                label = { Text("All") }
            )
            folderOptions.forEach { folder ->
                FilterChip(
                    selected = selectedFolder == folder,
                    onClick = { onFolderSelected(folder) },
                    label = { Text(folder.takeLastWhile { it != '/' }) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sort Button
            Box {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sort", style = MaterialTheme.typography.labelSmall)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    LibrarySortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            leadingIcon = {
                                if (option == sortOption) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            text = {
                                Text(
                                    when (option) {
                                        LibrarySortOption.TITLE_ASC -> "Title (A-Z)"
                                        LibrarySortOption.TITLE_DESC -> "Title (Z-A)"
                                        LibrarySortOption.ARTIST -> "Artist"
                                        LibrarySortOption.RECENTLY_ADDED -> "Recently Added"
                                        LibrarySortOption.DURATION -> "Duration"
                                    }
                                )
                            },
                            onClick = {
                                onSortOptionChange(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Create Playlist Button
            OutlinedButton(
                onClick = onCreatePlaylist,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Playlist", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LibrarySection(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaylistItem(
    modifier: Modifier = Modifier,
    playlist: Playlist,
    songCount: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$songCount songs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Playlist actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        onRename()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LibrarySongItem(
    song: Song,
    isFavorite: Boolean,
    isSelected: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleSelection: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onPlay() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AlbumArt(
            uri = song.albumArtUri,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        trailingAction?.invoke()
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            TextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                placeholder = { Text("Playlist name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(playlistName) },
                enabled = playlistName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistName by remember(currentName) { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Playlist") },
        text = {
            TextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                placeholder = { Text("Playlist name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(playlistName.trim()) },
                enabled = playlistName.trim().isNotBlank() && playlistName.trim() != currentName
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfirmDeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist") },
        text = { Text("Delete \"$playlistName\"? This cannot be undone.") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddSongsToPlaylistDialog(
    playlistName: String,
    songs: List<Song>,
    existingSongIds: Set<Long>,
    onDismiss: () -> Unit,
    onAdd: (List<Long>) -> Unit
) {
    val candidates = remember(songs, existingSongIds) {
        songs.filterNot { it.id in existingSongIds }
    }
    val selectedIds = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Songs to $playlistName") },
        text = {
            if (candidates.isEmpty()) {
                Text("All songs are already in this playlist.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(candidates, key = { it.id }) { song ->
                        val checked = song.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!selectedIds.add(song.id)) {
                                        selectedIds.remove(song.id)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (song.id !in selectedIds) selectedIds.add(song.id)
                                    } else {
                                        selectedIds.remove(song.id)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    song.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(selectedIds.toList()) }, enabled = selectedIds.isNotEmpty()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}




















