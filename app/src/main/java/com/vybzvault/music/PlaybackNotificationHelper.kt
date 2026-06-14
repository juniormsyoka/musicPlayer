package com.vybzvault.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.Coil
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackNotificationHelper(
    private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY     = "com.vybzvault.music.PLAY"
        const val ACTION_PAUSE    = "com.vybzvault.music.PAUSE"
        const val ACTION_NEXT     = "com.vybzvault.music.NEXT"
        const val ACTION_PREVIOUS = "com.vybzvault.music.PREVIOUS"
        const val ACTION_STOP     = "com.vybzvault.music.STOP"
        const val ACTION_REPEAT   = "com.vybzvault.music.REPEAT"
        const val ACTION_SHUFFLE  = "com.vybzvault.music.SHUFFLE"
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls music playback"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun loadAlbumArt(song: Song): Bitmap? = withContext(Dispatchers.IO) {
        if (song.albumArtUri == null) return@withContext null
        return@withContext try {
            val drawable = Coil.imageLoader(context).execute(
                ImageRequest.Builder(context)
                    .data(song.albumArtUri)
                    .allowHardware(false) // must be false — hardware bitmaps can't be drawn to Canvas
                    .size(512, 512)
                    .build()
            ).drawable
            drawable?.let { convertDrawableToBitmap(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun convertDrawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            return Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).also {
                Canvas(it).drawColor(Color.DKGRAY)
            }
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun buildNotification(
        song: Song,
        isPlaying: Boolean,
        repeatMode: RepeatMode,
        shuffleEnabled: Boolean,
        positionMs: Long,
        durationMs: Long,
        albumArt: Bitmap? = null,
        // THE ROOT CAUSE FIX:
        // MediaSession token is REQUIRED for the OS to render album art, progress bar,
        // and compact actions in a media notification on API 21+.
        // Without setMediaSession(), the system treats this as a plain notification
        // and silently ignores all MediaStyle-specific rendering (art, progress, actions).
        mediaSession: MediaSessionCompat
    ): Notification {

        val playAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playIcon   = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playText   = if (isPlaying) "Pause" else "Play"

        val repeatIcon = when (repeatMode) {
            RepeatMode.OFF -> android.R.drawable.ic_menu_revert
            RepeatMode.ALL -> android.R.drawable.ic_menu_rotate
            RepeatMode.ONE -> android.R.drawable.ic_menu_more
        }
        val shuffleIcon = if (shuffleEnabled)
            android.R.drawable.ic_menu_sort_by_size
        else
            android.R.drawable.ic_menu_sort_alphabetically

        // Work in seconds to prevent Int overflow on tracks > ~35 min
        val durationSec   = (durationMs / 1000L).toInt().coerceAtLeast(0)
        val positionSec   = (positionMs / 1000L).toInt().coerceIn(0, durationSec)
        val indeterminate = durationSec <= 0

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(albumArt)
            .setContentIntent(createOpenAppIntent())
            .setDeleteIntent(createServiceIntent(ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setProgress(durationSec, positionSec, indeterminate)
            // Action indices: 0=Previous  1=Play/Pause  2=Next  3=Repeat  4=Shuffle
            .addAction(NotificationCompat.Action(
                android.R.drawable.ic_media_previous, "Previous",
                createServiceIntent(ACTION_PREVIOUS, 1)
            ))
            .addAction(NotificationCompat.Action(
                playIcon, playText,
                createServiceIntent(playAction, 2)
            ))
            .addAction(NotificationCompat.Action(
                android.R.drawable.ic_media_next, "Next",
                createServiceIntent(ACTION_NEXT, 3)
            ))
            .addAction(NotificationCompat.Action(
                repeatIcon, "Repeat",
                createServiceIntent(ACTION_REPEAT, 4)
            ))
            .addAction(NotificationCompat.Action(
                shuffleIcon, "Shuffle",
                createServiceIntent(ACTION_SHUFFLE, 5)
            ))

        // Compact view supports max 3 indices — Previous, Play/Pause, Next
        // setMediaSession() is what tells the OS "this is a media notification":
        //   - unlocks the album art panel
        //   - enables the seek/progress bar
        //   - activates compact action rendering
        //   - registers with the system media controller (lock screen, Bluetooth, etc.)
        val style = MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setMediaSession(mediaSession.sessionToken)

        builder.setStyle(style)
        return builder.build()
    }

    private fun createOpenAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createServiceIntent(action: String, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun updateNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}