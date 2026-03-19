package com.example.music

import android.net.Uri
import androidx.compose.runtime.Stable

@Stable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dateAddedSec: Long,
    val contentUri: Uri,
    val folder: String,
    val albumArtUri: Uri?,
    val trackNumber: Int = 0,
    val year: Int = 0
)

@Stable
data class Playlist(
    val name: String,
    val songIds: List<Long>
)