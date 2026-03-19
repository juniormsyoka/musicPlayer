package com.example.music

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : Service(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE     = "com.example.music.action.PLAY_PAUSE"
        const val ACTION_NEXT           = "com.example.music.action.NEXT"
        const val ACTION_PREVIOUS       = "com.example.music.action.PREVIOUS"
        const val ACTION_STOP           = "com.example.music.action.STOP"
        const val ACTION_REPEAT_TOGGLE  = "com.example.music.action.REPEAT_TOGGLE"
        const val ACTION_SHUFFLE_TOGGLE = "com.example.music.action.SHUFFLE_TOGGLE"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val binder = MusicBinder()
    private val scope  = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var audioManager: AudioManager
    private lateinit var notificationHelper: PlaybackNotificationHelper

    // THE FIX: MediaSession is the bridge between your service and the Android
    // media notification system. It must be created, kept active, and its token
    // passed to MediaStyle — otherwise album art, progress bar, and actions
    // are all suppressed by the OS regardless of what you set on the builder.
    private lateinit var mediaSession: MediaSessionCompat

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var progressJob: Job? = null

    private var currentIndex = -1
    private var shuffledIndices: List<Int> = emptyList()
    private var currentAlbumArt: Bitmap? = null

    private val _playbackState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playbackState: StateFlow<PlayerState> = _playbackState.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatModeFlow: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabledFlow: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        audioManager       = getSystemService(AudioManager::class.java)
        notificationHelper = PlaybackNotificationHelper(this)

        // Create and activate the MediaSession
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
    }

    // MediaSessionCompat.Callback lets the system (lock screen, Bluetooth headset,
    // Android Auto, Assistant) control playback directly without going through intents.
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay()         { resume() }
        override fun onPause()        { pause() }
        override fun onSkipToNext()   { skipNext() }
        override fun onSkipToPrevious() { skipPrevious() }
        override fun onStop()         { stopPlayback() }
        override fun onSeekTo(pos: Long) { seekTo(pos) }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                when (_playbackState.value) {
                    is PlayerState.Playing -> pause()
                    is PlayerState.Paused  -> resume()
                    else -> Unit
                }
            }
            PlaybackNotificationHelper.ACTION_PLAY  -> {
                if (_playbackState.value is PlayerState.Paused) resume()
            }
            PlaybackNotificationHelper.ACTION_PAUSE -> {
                if (_playbackState.value is PlayerState.Playing) pause()
            }
            ACTION_NEXT,
            PlaybackNotificationHelper.ACTION_NEXT     -> skipNext()
            ACTION_PREVIOUS,
            PlaybackNotificationHelper.ACTION_PREVIOUS -> skipPrevious()
            ACTION_STOP,
            PlaybackNotificationHelper.ACTION_STOP     -> stopPlayback()
            ACTION_REPEAT_TOGGLE,
            PlaybackNotificationHelper.ACTION_REPEAT   -> setRepeatMode(nextRepeatMode(_repeatMode.value))
            ACTION_SHUFFLE_TOGGLE,
            PlaybackNotificationHelper.ACTION_SHUFFLE  -> setShuffle(!_shuffleEnabled.value)
        }
        return START_STICKY
    }

    fun playSong(song: Song, queue: List<Song>) {
        val resolvedQueue  = queue.ifEmpty { listOf(song) }
        val indexInQueue   = resolvedQueue.indexOfFirst { it.id == song.id }
        if (indexInQueue < 0) return

        _queue.value  = resolvedQueue
        currentIndex  = indexInQueue
        currentAlbumArt = null   // clear stale art before loading new song

        if (!requestAudioFocus()) {
            _playbackState.value = PlayerState.Error("Failed to get audio focus", song)
            refreshNotificationForCurrentState()
            return
        }

        _playbackState.value = PlayerState.Preparing
        releasePlayer()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setOnPreparedListener { player ->
                player.start()
                _playbackState.value = PlayerState.Playing(song, 0L, song.durationMs)
                updateMediaSessionMetadata(song)
                updateMediaSessionPlaybackState(isPlaying = true, positionMs = 0L)
                startProgressUpdates()
                startPlaybackForeground(song)
            }

            setOnCompletionListener { onSongCompleted() }

            setOnErrorListener { _, what, _ ->
                _playbackState.value = PlayerState.Error("Playback error: $what", song)
                refreshNotificationForCurrentState()
                true
            }

            setDataSource(this@MusicService, song.contentUri)
            prepareAsync()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        val state = _playbackState.value
        if (state is PlayerState.Playing) {
            _playbackState.value = PlayerState.Paused(state.song, state.position, state.duration)
            updateMediaSessionPlaybackState(isPlaying = false, positionMs = state.position)
            stopProgressUpdates()
            stopForeground(STOP_FOREGROUND_DETACH)
            refreshNotificationForCurrentState()
        }
    }

    fun resume() {
        mediaPlayer?.start()
        val state = _playbackState.value
        if (state is PlayerState.Paused) {
            _playbackState.value = PlayerState.Playing(state.song, state.position, state.duration)
            updateMediaSessionPlaybackState(isPlaying = true, positionMs = state.position)
            startProgressUpdates()
            startPlaybackForeground(state.song)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.coerceAtLeast(0L).toInt())
        updatePlaybackPosition()
    }

    fun skipNext() {
        val songs = _queue.value
        if (songs.isEmpty() || currentIndex !in songs.indices) return
        val nextIndex = if (_shuffleEnabled.value) nextShuffledIndex()
        else (currentIndex + 1) % songs.size
        playSong(songs[nextIndex], songs)
    }

    fun skipPrevious() {
        val songs = _queue.value
        if (songs.isEmpty() || currentIndex !in songs.indices) return
        val state = _playbackState.value
        if (state is PlayerState.Playing && state.position > PlayerConstants.SEEK_BACK_THRESHOLD_MS) {
            seekTo(0)
            return
        }
        val prevIndex = if (currentIndex == 0) songs.lastIndex else currentIndex - 1
        playSong(songs[prevIndex], songs)
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        refreshNotificationForCurrentState()
    }

    fun setShuffle(enabled: Boolean) {
        _shuffleEnabled.value = enabled
        shuffledIndices = if (enabled) _queue.value.indices.shuffled() else emptyList()
        refreshNotificationForCurrentState()
    }

    fun updateQueue(queue: List<Song>) {
        _queue.value = queue
        if (currentIndex !in queue.indices) {
            currentIndex = queue.indexOfFirst { it.id == currentSong(_playbackState.value)?.id }
        }
        if (_shuffleEnabled.value) shuffledIndices = queue.indices.shuffled()
    }

    // Update MediaSession metadata so the OS knows the current track details.
    // This is what populates lock screen info and Bluetooth display.
    private fun updateMediaSessionMetadata(song: Song) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,  song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,  song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.durationMs)
            .also { builder ->
                currentAlbumArt?.let { art ->
                    builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                }
            }
            .build()
        mediaSession.setMetadata(metadata)
    }

    // Keep PlaybackState in sync so the system progress bar and transport controls
    // reflect the real playback position.
    private fun updateMediaSessionPlaybackState(isPlaying: Boolean, positionMs: Long) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING
        else           PlaybackStateCompat.STATE_PAUSED

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY              or
                        PlaybackStateCompat.ACTION_PAUSE             or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE        or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT      or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS  or
                        PlaybackStateCompat.ACTION_SEEK_TO           or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, positionMs, 1.0f)
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun onSongCompleted() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                val song = _queue.value.getOrNull(currentIndex)
                if (song != null) playSong(song, _queue.value)
            }
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                if (currentIndex < _queue.value.lastIndex) skipNext() else pause()
            }
        }
    }

    private fun nextShuffledIndex(): Int {
        if (shuffledIndices.isEmpty()) return 0
        val pos     = shuffledIndices.indexOf(currentIndex)
        val nextPos = if (pos < 0) 0 else (pos + 1) % shuffledIndices.size
        return shuffledIndices[nextPos]
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = scope.launch {
            while (isActive) {
                updatePlaybackPosition()
                delay(PlayerConstants.PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updatePlaybackPosition() {
        val player = mediaPlayer ?: return
        when (val state = _playbackState.value) {
            is PlayerState.Playing -> {
                val pos = player.currentPosition.toLong().coerceAtLeast(0L)
                _playbackState.value = state.copy(position = pos)
                updateMediaSessionPlaybackState(isPlaying = true, positionMs = pos)
                refreshNotificationForCurrentState()
            }
            is PlayerState.Paused -> {
                // Don't hammer the notification while paused
            }
            else -> Unit
        }
    }

    // Single structured coroutine: load art on IO, then post foreground on Main.
    // No nested launches — if the scope is cancelled mid-load, everything stops cleanly.
    private fun startPlaybackForeground(song: Song) {
        scope.launch {
            val art = withContext(Dispatchers.IO) {
                notificationHelper.loadAlbumArt(song)
            }
            currentAlbumArt = art

            // Update metadata with album art now that we have it
            updateMediaSessionMetadata(song)

            val state    = _playbackState.value
            val duration = currentDuration(state).takeIf { it > 0L } ?: song.durationMs
            val position = currentPosition(state)

            val notification = notificationHelper.buildNotification(
                song           = song,
                isPlaying      = state is PlayerState.Playing,
                repeatMode     = _repeatMode.value,
                shuffleEnabled = _shuffleEnabled.value,
                positionMs     = position,
                durationMs     = duration,
                albumArt       = currentAlbumArt,
                mediaSession   = mediaSession   // pass the session token
            )
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun refreshNotificationForCurrentState() {
        if (!canPostNotifications()) return
        val state = _playbackState.value
        val song  = currentSong(state) ?: return

        notificationHelper.updateNotification(
            notificationHelper.buildNotification(
                song           = song,
                isPlaying      = state is PlayerState.Playing,
                repeatMode     = _repeatMode.value,
                shuffleEnabled = _shuffleEnabled.value,
                positionMs     = currentPosition(state),
                durationMs     = currentDuration(state).takeIf { it > 0L } ?: song.durationMs,
                albumArt       = currentAlbumArt,
                mediaSession   = mediaSession   // pass the session token
            )
        )
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun stopPlayback() {
        stopProgressUpdates()
        releasePlayer()
        _playbackState.value = PlayerState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayer() {
        mediaPlayer?.runCatching {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
        stopProgressUpdates()
        abandonAudioFocus()
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(this)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT          -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Unit
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (_playbackState.value is PlayerState.Paused) resume()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressUpdates()
        releasePlayer()
        mediaSession.isActive = false
        mediaSession.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        scope.cancel()
    }
}