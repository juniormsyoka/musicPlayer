package com.example.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun PlaylistSection(
    playlists: List<Playlist>,
    songsById: Map<Long, Song>,
    onMovePlaylist: (Int, Int) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onDeletePlaylist: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        playlists.forEachIndexed { index, playlist ->
            PlaylistItem(
                playlist = playlist,
                songsById = songsById,
                index = index,
                totalCount = playlists.size,
                onMoveUp = { if (index > 0) onMovePlaylist(index, index - 1) },
                onMoveDown = { if (index < playlists.lastIndex) onMovePlaylist(index, index + 1) },
                onDeletePlaylist = { onDeletePlaylist(playlist.name) },
                onRemoveSong = { songId -> onRemoveSongFromPlaylist(playlist.name, songId) }
            )
        }
    }
}

@Composable
fun PlaylistItem(
    playlist: Playlist,
    songsById: Map<Long, Song>,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onRemoveSong: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val songs = playlist.songIds.mapNotNull { songsById[it] }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Playlist header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 2×2 collage art
            PlaylistArtCollage(
                songs = songs.take(4)
            )

            // Name + count
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
                Text(
                    text = "${playlist.songIds.size} song${if (playlist.songIds.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // Reorder + delete icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index > 0) {
                    SmallIconBtn(icon = Icons.Default.KeyboardArrowUp, desc = "Move up", onClick = onMoveUp)
                }
                if (index < totalCount - 1) {
                    SmallIconBtn(icon = Icons.Default.KeyboardArrowDown, desc = "Move down", onClick = onMoveDown)
                }
                SmallIconBtn(
                    icon = Icons.Default.Delete,
                    desc = "Delete playlist",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeletePlaylist
                )
            }
        }

        // Expandable song list
        if (expanded && songs.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                songs.take(10).forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                        IconButton(
                            onClick = { onRemoveSong(song.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                if (songs.size > 10) {
                    Text(
                        text = "+${songs.size - 10} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PlaylistArtCollage(songs: List<Song>) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        if (songs.isEmpty()) return@Box

        val cells = (songs + List(maxOf(0, 4 - songs.size)) { songs.first() }).take(4)
        Column {
            Row(modifier = Modifier.height(24.dp)) {
                CollageCell(cells[0], Modifier.weight(1f).fillMaxHeight())
                Spacer(modifier = Modifier.width(1.dp))
                CollageCell(cells[1], Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(modifier = Modifier.height(24.dp)) {
                CollageCell(cells[2], Modifier.weight(1f).fillMaxHeight())
                Spacer(modifier = Modifier.width(1.dp))
                CollageCell(cells[3], Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun CollageCell(song: Song, modifier: Modifier = Modifier) {
    if (song.albumArtUri != null) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                Color(
                    red = (song.title.hashCode() and 0xFF) / 255f * 0.5f + 0.2f,
                    green = ((song.title.hashCode() shr 8) and 0xFF) / 255f * 0.3f + 0.15f,
                    blue = 0.3f
                )
            )
        )
    }
}