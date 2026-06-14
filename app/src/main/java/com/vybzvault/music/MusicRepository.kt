package com.vybzvault.music

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        try {
            querySongs()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun querySongs(): List<Song> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val relativePathIndex = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            val dataPathIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val albumIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val trackIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val yearIndex = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val relativePath = if (relativePathIndex >= 0) cursor.getString(relativePathIndex) else null
                val legacyPath = if (dataPathIndex >= 0) cursor.getString(dataPathIndex) else null
                val folder = resolveFolder(relativePath, legacyPath)
                val albumId = if (albumIdIndex >= 0) cursor.getLong(albumIdIndex) else -1L
                val trackNumber = if (trackIndex >= 0) cursor.getInt(trackIndex) else 0
                val year = if (yearIndex >= 0) cursor.getInt(yearIndex) else 0

                songs += Song(
                    id = id,
                    title = cursor.getString(titleIndex) ?: "Unknown title",
                    artist = cursor.getString(artistIndex) ?: "Unknown artist",
                    album = cursor.getString(albumIndex) ?: "Unknown album",
                    durationMs = cursor.getLong(durationIndex),
                    dateAddedSec = cursor.getLong(dateAddedIndex),
                    contentUri = ContentUris.withAppendedId(collection, id),
                    folder = folder,
                    albumArtUri = buildAlbumArtUri(albumId),
                    trackNumber = trackNumber,
                    year = year
                )
            }
        }

        return songs
    }

    private fun resolveFolder(relativePath: String?, fullPath: String?): String {
        if (!relativePath.isNullOrBlank()) {
            return relativePath.trimEnd('/').ifBlank { "Root" }
        }
        if (fullPath.isNullOrBlank()) {
            return "Unknown folder"
        }
        val normalized = fullPath.replace('\\', '/')
        val cutIndex = normalized.lastIndexOf('/')
        return if (cutIndex > 0) normalized.substring(0, cutIndex) else "Unknown folder"
    }

    private fun buildAlbumArtUri(albumId: Long): Uri? {
        if (albumId <= 0) return null
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    }
}