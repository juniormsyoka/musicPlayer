package com.vybzvault.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vybzvault.music.LibraryState
import com.vybzvault.music.LibrarySortOption
import com.vybzvault.music.Playlist
import com.vybzvault.music.Song
import com.vybzvault.music.ui.components.AlbumArt
import com.vybzvault.music.ui.theme.LocalThemeVisuals
import kotlinx.coroutines.launch

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun LibraryScreen(
    libraryState: LibraryState,
    selectedFolder: String?,
    showAlbumArt: Boolean,
    sortOption: LibrarySortOption,
    selectedSongIds: Set<Long>,
    currentPlayingSongId: Long?,
    onFolderSelected: (String?) -> Unit,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onAddSongToPlaylist: (String, Long) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val themeVisuals = LocalThemeVisuals.current

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylistName by remember { mutableStateOf<String?>(null) }
    var playlistToRename by remember { mutableStateOf<String?>(null) }
    var playlistToDelete by remember { mutableStateOf<String?>(null) }
    var playlistForAddSongs by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val songById by remember(libraryState.songs) {
        derivedStateOf { libraryState.songs.associateBy { it.id } }
    }
    val selectedPlaylist by remember(selectedPlaylistName, libraryState.playlists) {
        derivedStateOf { libraryState.playlists.firstOrNull { it.name == selectedPlaylistName } }
    }
    val selectedPlaylistSongs by remember(selectedPlaylist, songById) {
        derivedStateOf { selectedPlaylist?.songIds?.mapNotNull(songById::get).orEmpty() }
    }
    val displayedSongs by remember(selectedFolder, libraryState.songs) {
        derivedStateOf {
            if (selectedFolder != null) libraryState.songs.filter { it.folder == selectedFolder }
            else libraryState.songs
        }
    }
    val sortedSongs by remember(displayedSongs, sortOption) {
        derivedStateOf {
            when (sortOption) {
                LibrarySortOption.TITLE_ASC -> displayedSongs.sortedBy { it.title }
                LibrarySortOption.TITLE_DESC -> displayedSongs.sortedByDescending { it.title }
                LibrarySortOption.ARTIST -> displayedSongs.sortedWith(
                    compareBy<Song> { it.artist }.thenBy { it.title }
                )
                LibrarySortOption.RECENTLY_ADDED -> displayedSongs.sortedByDescending { it.dateAddedSec }
                LibrarySortOption.DURATION -> displayedSongs.sortedByDescending { it.durationMs }
            }
        }
    }
    val playlistSongCounts by remember(libraryState.playlists, songById) {
        derivedStateOf {
            libraryState.playlists.associate { p ->
                p.name to p.songIds.count(songById::containsKey)
            }
        }
    }

    // Jump-to-current FAB logic
    val currentTrackVisible by remember(currentPlayingSongId, listState.layoutInfo.visibleItemsInfo) {
        derivedStateOf {
            val id = currentPlayingSongId ?: return@derivedStateOf false
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { it.key?.toString() }.toSet()
                .contains("all-$id")
        }
    }
    var showJumpFab by remember { mutableStateOf(false) }
    LaunchedEffect(currentPlayingSongId, currentTrackVisible, selectedPlaylistName) {
        showJumpFab = selectedPlaylistName == null && currentPlayingSongId != null && !currentTrackVisible
    }
    LaunchedEffect(selectedPlaylistName, libraryState.playlists) {
        if (selectedPlaylistName != null && selectedPlaylist == null) selectedPlaylistName = null
    }

    fun scrollToCurrentTrack() {
        val targetId = currentPlayingSongId ?: return
        val idx = sortedSongs.indexOfFirst { it.id == targetId }.takeIf { it >= 0 } ?: return
        val offset = if (selectedFolder == null) 4 else 1
        scope.launch { listState.animateScrollToItem(offset + idx) }
    }

    // ── Routing: playlist detail vs main list ──────────────────────────────
    if (selectedPlaylistName != null) {
        val playlist = selectedPlaylist ?: run { selectedPlaylistName = null; return }
        val playlistName = selectedPlaylistName ?: return
        PlaylistDetailScreen(
            playlist = playlist,
            playlistSongs = selectedPlaylistSongs,
            showAlbumArt = showAlbumArt,
            favorites = libraryState.favorites,
            selectedSongIds = selectedSongIds,
            currentPlayingSongId = currentPlayingSongId,
            onBack = { selectedPlaylistName = null },
            onPlaySong = onPlaySong,
            onPlayNext = onPlayNext,
            onToggleFavorite = onToggleFavorite,
            onToggleSongSelection = onToggleSongSelection,
            onRemoveSong = { songId -> onRemoveSongFromPlaylist(playlistName, songId) },
            onEditPlaylist = { playlistForAddSongs = playlistName },
            onRenamePlaylist = { playlistToRename = playlistName },
            onDeletePlaylist = { playlistToDelete = playlistName }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Theme-aware header ──────────────────────────────────────────────
                LibraryHeader(
                    libraryState = libraryState,
                    selectedFolder = selectedFolder,
                    sortOption = sortOption,
                    onFolderSelected = onFolderSelected,
                    onSortOptionChange = onSortOptionChange,
                    onCreatePlaylist = { showCreatePlaylistDialog = true }
                )

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Playlists section (top-level only) ──────────────────
                    if (selectedFolder == null) {
                        item {
                            SectionHeader(
                                title = "Playlists",
                                subtitle = "${libraryState.playlists.size} collections",
                                iconRes = Icons.Default.LibraryMusic
                            )
                        }

                        if (libraryState.playlists.isEmpty()) {
                            item { EmptyPlaylistsHint() }
                        } else {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    libraryState.playlists.forEach { playlist ->
                                        PlaylistCard(
                                            playlist = playlist,
                                            songCount = playlistSongCounts[playlist.name] ?: 0,
                                            onClick = { selectedPlaylistName = playlist.name },
                                            onRename = { playlistToRename = playlist.name },
                                            onDelete = { playlistToDelete = playlist.name }
                                        )
                                    }
                                }
                            }
                        }
                        item { DividerSpacer() }
                    }

                    // ── Songs section ────────────────────────────────────────
                    item {
                        SectionHeader(
                            title = "Songs",
                            subtitle = "${sortedSongs.size} tracks",
                            iconRes = Icons.Default.MusicNote
                        )
                    }

                    if (sortedSongs.isEmpty()) {
                        item { EmptySongsHint() }
                    } else {
                        items(sortedSongs, key = { "all-${it.id}" }) { song ->
                            SongRow(
                                song = song,
                                isFavorite = libraryState.favorites.contains(song.id),
                                showAlbumArt = showAlbumArt,
                                isSelected = selectedSongIds.contains(song.id),
                                isNowPlaying = song.id == currentPlayingSongId,
                                onPlay = { onPlaySong(song, sortedSongs) },
                                onPlayNext = { onPlayNext(song) },
                                onToggleFavorite = { onToggleFavorite(song.id) },
                                onToggleSelection = { onToggleSongSelection(song.id) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(112.dp)) }
                }
            }

            // ── Jump-to-current FAB ──────────────────────────────────────────
            if (showJumpFab) {
                ExtendedFloatingActionButton(
                    onClick = { scrollToCurrentTrack() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 96.dp)
                        .shadow(8.dp, RoundedCornerShape(50)),
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                    text = { Text("Now Playing", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showCreatePlaylistDialog) {
        PlaylistNameDialog(
            title = "New Playlist",
            confirmLabel = "Create",
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name -> onCreatePlaylist(name); showCreatePlaylistDialog = false }
        )
    }
    playlistToRename?.let { oldName ->
        PlaylistNameDialog(
            title = "Rename Playlist",
            initialValue = oldName,
            confirmLabel = "Save",
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
            existingSongIds = libraryState.playlists.firstOrNull { it.name == playlistName }
                ?.songIds.orEmpty().toSet(),
            onDismiss = { playlistForAddSongs = null },
            onAdd = { songIds ->
                songIds.forEach { onAddSongToPlaylist(playlistName, it) }
                playlistForAddSongs = null
            }
        )
    }
}

// ─── Library Header (Theme-Aware) ───────────────────────────────────────────

@Composable
private fun LibraryHeader(
    libraryState: LibraryState,
    selectedFolder: String?,
    sortOption: LibrarySortOption,
    onFolderSelected: (String?) -> Unit,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val folders = remember(libraryState.songs) {
        libraryState.songs.map { it.folder }.distinct().sorted()
    }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.surface.copy(alpha = 0.95f),
                        colors.background
                    )
                )
            )
            .border(
                width = 1.dp,
                color = colors.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
    ) {
        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Library",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "${libraryState.songs.size} songs  ·  ${libraryState.playlists.size} playlists",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sort
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceVariant)
                            .border(1.dp, colors.outline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        LibrarySortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    if (option == sortOption) Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                text = { Text(option.label()) },
                                onClick = { onSortOptionChange(option); showSortMenu = false }
                            )
                        }
                    }
                }
                // New playlist
                IconButton(
                    onClick = onCreatePlaylist,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New playlist",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Folder filter chips
        if (folders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(label = "All", selected = selectedFolder == null, onClick = { onFolderSelected(null) })
                folders.forEach { folder ->
                    FilterChip(
                        label = folder.takeLastWhile { it != '/' },
                        selected = selectedFolder == folder,
                        onClick = { onFolderSelected(folder) }
                    )
                }
            }
        }
    }
}

// ─── Section Header (Theme-Aware) ────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    iconRes: androidx.compose.ui.graphics.vector.ImageVector
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconRes,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

// ─── Divider / Spacer ────────────────────────────────────────────────────

@Composable
private fun DividerSpacer() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(1.dp)
            .background(colors.outlineVariant)
    )
}

// ─── Filter Chip (Theme-Aware) ──────────────────────────────────────────────

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.primary else colors.surfaceVariant)
            .border(1.dp, if (selected) colors.primary else colors.outline, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant
        )
    }
}

// ─── Playlist Card (Theme-Aware) ─────────────────────────────────────────────

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    songCount: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { onRename(); showMenu = false })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); showMenu = false })
                }
            }
        }
        Text(
            text = playlist.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$songCount songs",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

// ─── Song Row (Theme-Aware with Now-Playing Animation) ─────────────────────

@Composable
fun SongRow(
    song: Song,
    isFavorite: Boolean,
    showAlbumArt: Boolean,
    isSelected: Boolean,
    isNowPlaying: Boolean,
    onPlay: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    onToggleSelection: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    var showMenu by remember { mutableStateOf(false) }

    // Now-playing spectrum animation using theme colors
    val transition = if (isNowPlaying) rememberInfiniteTransition(label = "spectrum") else null
    val spectrumProgress by if (transition != null) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
            label = "spectrumProgress"
        )
    } else remember { mutableFloatStateOf(0f) }

    val spectrumColors = listOf(
        colors.primary,
        colors.secondary,
        colors.tertiary,
        colors.primary
    )
    val titleColor = when {
        isNowPlaying -> animatedSpectrumColor(spectrumProgress, spectrumColors)
        isSelected -> colors.primary
        else -> colors.onSurface
    }

    val bgColor = when {
        isNowPlaying -> colors.primaryContainer.copy(alpha = 0.5f)
        isSelected -> colors.surfaceVariant
        else -> Color.Transparent
    }
    val borderColor = if (isNowPlaying) colors.primary.copy(alpha = 0.5f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onPlay() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Album art / now-playing indicator
        Box(modifier = Modifier.size(44.dp)) {
            AlbumArt(
                uri = song.albumArtUri,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                showArtwork = showAlbumArt
            )
            if (isNowPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Now playing",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Title + artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontWeight = if (isNowPlaying) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                color = titleColor,
                fontStyle = if (isNowPlaying) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 12.sp,
                color = if (isNowPlaying) colors.primary else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Favorite
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                tint = if (isFavorite) colors.error else colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Trailing (e.g. remove from playlist)
        trailingAction?.invoke()

        // More menu
        if (onPlayNext != null) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Play next") },
                        onClick = { onPlayNext(); showMenu = false }
                    )
                }
            }
        }
    }
}

// ─── Empty States (Theme-Aware) ─────────────────────────────────────────────

@Composable
private fun EmptyPlaylistsHint() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No playlists yet — tap + to create one",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptySongsHint() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AudioFile,
            contentDescription = null,
            tint = colors.outline,
            modifier = Modifier.size(52.dp)
        )
        Text(
            text = "No songs in this folder",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}

// ─── Dialogs (Theme-Aware) ──────────────────────────────────────────────────

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialValue: String = "",
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var name by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = colors.onSurface) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name", color = colors.onSurfaceVariant) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outline,
                    cursorColor = colors.primary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotBlank() && name.trim() != initialValue.ifEmpty { null },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) }
        }
    )
}

@Composable
private fun ConfirmDeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist", fontWeight = FontWeight.Bold, color = colors.onSurface) },
        text = { Text("Delete \"$playlistName\"? This cannot be undone.", color = colors.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) }
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
    val colors = MaterialTheme.colorScheme
    val candidates = remember(songs, existingSongIds) { songs.filterNot { it.id in existingSongIds } }
    val selectedIds = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add to $playlistName",
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            if (candidates.isEmpty()) {
                Text("All songs are already in this playlist.", color = colors.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                    items(candidates, key = { it.id }) { song ->
                        val checked = song.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (checked) colors.primaryContainer else Color.Transparent)
                                .clickable {
                                    if (!selectedIds.add(song.id)) selectedIds.remove(song.id)
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { on ->
                                    if (on) {
                                        if (song.id !in selectedIds) selectedIds.add(song.id)
                                    } else selectedIds.remove(song.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(song.title, fontWeight = FontWeight.Medium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) { Text("Add ${if (selectedIds.isNotEmpty()) "(${selectedIds.size})" else ""}") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) }
        }
    )
}

// ─── Playlist Detail (Theme-Aware) ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailScreen(
    playlist: Playlist,
    playlistSongs: List<Song>,
    showAlbumArt: Boolean,
    favorites: Set<Long>,
    selectedSongIds: Set<Long>,
    currentPlayingSongId: Long?,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onEditPlaylist: () -> Unit,
    onRenamePlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showEditMenu by remember { mutableStateOf(false) }
    val coverArtUri = playlistSongs.firstOrNull()?.albumArtUri

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = playlist.name,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.onSurfaceVariant)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
        )

        // Playlist hero card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showAlbumArt && coverArtUri != null) {
                AlbumArt(
                    uri = coverArtUri,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)),
                    showArtwork = true
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QueueMusic, null, tint = colors.primary, modifier = Modifier.size(30.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.onSurface)
                Text("${playlistSongs.size} songs", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = { playlistSongs.firstOrNull()?.let { onPlaySong(it, playlistSongs) } },
                enabled = playlistSongs.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.primaryContainer)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Play", color = colors.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
            }
            FilledTonalButton(
                onClick = {
                    if (playlistSongs.isNotEmpty()) {
                        val shuffled = playlistSongs.shuffled()
                        onPlaySong(shuffled.first(), shuffled)
                    }
                },
                enabled = playlistSongs.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.primaryContainer)
            ) {
                Icon(Icons.Default.Shuffle, null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Shuffle", color = colors.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
            }
            Box(modifier = Modifier.weight(1f)) {
                FilledTonalButton(
                    onClick = { showEditMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.surfaceVariant)
                ) {
                    Icon(Icons.Default.Edit, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit", color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
                DropdownMenu(expanded = showEditMenu, onDismissRequest = { showEditMenu = false }) {
                    DropdownMenuItem(text = { Text("Add songs") }, onClick = { onEditPlaylist(); showEditMenu = false })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { onRenamePlaylist(); showEditMenu = false })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { onDeletePlaylist(); showEditMenu = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        SectionHeader(
            title = "Songs",
            subtitle = "${playlistSongs.size} tracks",
            iconRes = Icons.Default.MusicNote
        )

        if (playlistSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("This playlist is empty. Use Edit to add songs.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 112.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlistSongs, key = { "playlist-${playlist.name}-${it.id}" }) { song ->
                    SongRow(
                        song = song,
                        isFavorite = favorites.contains(song.id),
                        showAlbumArt = showAlbumArt,
                        isSelected = selectedSongIds.contains(song.id),
                        isNowPlaying = song.id == currentPlayingSongId,
                        onPlay = { onPlaySong(song, playlistSongs) },
                        onPlayNext = { onPlayNext(song) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onToggleSelection = { onToggleSongSelection(song.id) },
                        trailingAction = {
                            IconButton(
                                onClick = { onRemoveSong(song.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove",
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
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun LibrarySortOption.label() = when (this) {
    LibrarySortOption.TITLE_ASC -> "Title (A–Z)"
    LibrarySortOption.TITLE_DESC -> "Title (Z–A)"
    LibrarySortOption.ARTIST -> "Artist"
    LibrarySortOption.RECENTLY_ADDED -> "Recently Added"
    LibrarySortOption.DURATION -> "Duration"
}

private fun animatedSpectrumColor(progress: Float, colors: List<Color>): Color {
    if (colors.isEmpty()) return Color.Unspecified
    if (colors.size == 1) return colors.first()
    val clamped = (progress % 1f).coerceIn(0f, 0.999999f)
    val scaled = clamped * (colors.size - 1)
    val index = scaled.toInt().coerceIn(0, colors.size - 2)
    return lerp(colors[index], colors[index + 1], scaled - index)
}