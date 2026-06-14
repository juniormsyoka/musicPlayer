package com.vybzvault.music

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

        const val ACTION_PLAY_PAUSE = "com.vybzvault.music.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.vybzvault.music.action.NEXT"
        const val ACTION_PREVIOUS = "com.vybzvault.music.action.PREVIOUS"
        const val ACTION_STOP = "com.vybzvault.music.action.STOP"
        const val ACTION_REPEAT_TOGGLE = "com.vybzvault.music.action.REPEAT_TOGGLE"
        const val ACTION_SHUFFLE_TOGGLE = "com.vybzvault.music.action.SHUFFLE_TOGGLE"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val binder = MusicBinder()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var audioManager: AudioManager
    private lateinit var notificationHelper: PlaybackNotificationHelper
    private lateinit var mediaSession: MediaSessionCompat

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var progressJob: Job? = null
    private var currentIndex = -1
    private var shuffledIndices: List<Int> = emptyList()
    private var currentAlbumArt: Bitmap? = null
    private var pendingRestore: PendingRestore? = null

    private data class PendingRestore(
        val positionMs: Long,
        val shouldPlay: Boolean
    )

    private val _playbackState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playbackState: StateFlow<PlayerState> = _playbackState.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatModeFlow: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabledFlow: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _audioSessionIdFlow = MutableStateFlow(0)
    val audioSessionIdFlow: StateFlow<Int> = _audioSessionIdFlow.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AudioManager::class.java)
        notificationHelper = PlaybackNotificationHelper(this)
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
        PlayerRepository.attachService(this)
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            resume()
        }

        override fun onPause() {
            pause()
        }

        override fun onSkipToNext() {
            skipNext()
        }

        override fun onSkipToPrevious() {
            skipPrevious()
        }

        override fun onSeekTo(pos: Long) {
            seekTo(pos)
        }

        override fun onStop() {
            stopPlayback()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                when (_playbackState.value) {
                    is PlayerState.Playing -> pause()
                    is PlayerState.Paused -> resume()
                    else -> Unit
                }
            }

            ACTION_NEXT,
            PlaybackNotificationHelper.ACTION_NEXT -> skipNext()

            ACTION_PREVIOUS,
            PlaybackNotificationHelper.ACTION_PREVIOUS -> skipPrevious()

            ACTION_STOP,
            PlaybackNotificationHelper.ACTION_STOP -> stopPlayback()

            ACTION_REPEAT_TOGGLE,
            PlaybackNotificationHelper.ACTION_REPEAT -> setRepeatMode(nextRepeatMode(_repeatMode.value))

            ACTION_SHUFFLE_TOGGLE,
            PlaybackNotificationHelper.ACTION_SHUFFLE -> setShuffle(!_shuffleEnabled.value)

            PlaybackNotificationHelper.ACTION_PLAY -> if (_playbackState.value is PlayerState.Paused) resume()
            PlaybackNotificationHelper.ACTION_PAUSE -> if (_playbackState.value is PlayerState.Playing) pause()
        }
        return START_STICKY
    }

    fun currentAudioSessionId(): Int = mediaPlayer?.audioSessionId ?: 0

    fun playSong(song: Song, queue: List<Song>) {
        val resolvedQueue = queue.ifEmpty { listOf(song) }
        val indexInQueue = resolvedQueue.indexOfFirst { it.id == song.id }
        if (indexInQueue < 0) return

        _queue.value = resolvedQueue
        currentIndex = indexInQueue
        if (_shuffleEnabled.value) {
            shuffledIndices = rebuildShuffledIndices(currentIndex, resolvedQueue.size)
        }

        if (!requestAudioFocus()) {
            _playbackState.value = PlayerState.Error("Failed to get audio focus", song)
            refreshNotificationForCurrentState()
            return
        }

        _playbackState.value = PlayerState.Preparing
        currentAlbumArt = null
        releasePlayer()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setOnPreparedListener { player ->
                _audioSessionIdFlow.value = player.audioSessionId

                val restore = pendingRestore
                if (restore != null) {
                    val safePosition = restore.positionMs.coerceAtLeast(0L).coerceAtMost(song.durationMs)
                    if (safePosition > 0L) {
                        player.seekTo(safePosition.toInt())
                    }

                    if (restore.shouldPlay) {
                        player.start()
                        _playbackState.value = PlayerState.Playing(song, safePosition, song.durationMs)
                        updateMediaSessionPlaybackState(isPlaying = true, positionMs = safePosition)
                        startProgressUpdates()
                    } else {
                        player.start()
                        player.pause()
                        _playbackState.value = PlayerState.Paused(song, safePosition, song.durationMs)
                        updateMediaSessionPlaybackState(isPlaying = false, positionMs = safePosition)
                        stopProgressUpdates()
                    }
                    pendingRestore = null
                } else {
                    player.start()
                    _playbackState.value = PlayerState.Playing(song, 0L, song.durationMs)
                    updateMediaSessionPlaybackState(isPlaying = true, positionMs = 0L)
                    startProgressUpdates()
                }

                updateMediaSessionMetadata(song)
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

    fun restorePlayback(song: Song, queue: List<Song>, positionMs: Long, shouldPlay: Boolean) {
        pendingRestore = PendingRestore(
            positionMs = positionMs,
            shouldPlay = shouldPlay
        )
        playSong(song, queue)
    }

    fun pause() {
        val player = mediaPlayer ?: return
        val state = _playbackState.value
        if (state !is PlayerState.Playing) return

        player.pause()
        val position = player.currentPosition.toLong().coerceAtLeast(0L)
        _playbackState.value = PlayerState.Paused(state.song, position, state.duration)
        updateMediaSessionPlaybackState(isPlaying = false, positionMs = position)
        stopProgressUpdates()
        refreshNotificationForCurrentState()
    }

    fun resume() {
        val player = mediaPlayer ?: return
        val state = _playbackState.value
        if (state !is PlayerState.Paused) return

        if (!requestAudioFocus()) return

        player.start()
        _playbackState.value = PlayerState.Playing(state.song, state.position, state.duration)
        updateMediaSessionPlaybackState(isPlaying = true, positionMs = state.position)
        startProgressUpdates()
        refreshNotificationForCurrentState()
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        val safePosition = positionMs.coerceAtLeast(0L)
        player.seekTo(safePosition.toInt())

        when (val state = _playbackState.value) {
            is PlayerState.Playing -> {
                _playbackState.value = state.copy(position = safePosition)
                updateMediaSessionPlaybackState(true, safePosition)
            }

            is PlayerState.Paused -> {
                _playbackState.value = state.copy(position = safePosition)
                updateMediaSessionPlaybackState(false, safePosition)
            }

            else -> Unit
        }
        refreshNotificationForCurrentState()
    }

    fun skipNext() {
        val songs = _queue.value
        if (songs.isEmpty() || currentIndex !in songs.indices) return
        val nextIndex = if (_shuffleEnabled.value) nextShuffledIndex() else (currentIndex + 1) % songs.size
        playSong(songs[nextIndex], songs)
    }

    fun skipPrevious() {
        val songs = _queue.value
        if (songs.isEmpty() || currentIndex !in songs.indices) return

        val state = _playbackState.value
        if (state is PlayerState.Playing && state.position > PlayerConstants.SEEK_BACK_THRESHOLD_MS) {
            seekTo(0L)
            return
        }

        val previous = if (currentIndex == 0) songs.lastIndex else currentIndex - 1
        playSong(songs[previous], songs)
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        refreshNotificationForCurrentState()
    }

    fun setShuffle(enabled: Boolean) {
        _shuffleEnabled.value = enabled
        shuffledIndices = if (enabled) {
            rebuildShuffledIndices(currentIndex, _queue.value.size)
        } else {
            emptyList()
        }
        refreshNotificationForCurrentState()
    }

    fun updateQueue(queue: List<Song>) {
        _queue.value = queue
        val nowPlayingId = currentSong(_playbackState.value)?.id
        currentIndex = queue.indexOfFirst { it.id == nowPlayingId }

        if (_shuffleEnabled.value) {
            shuffledIndices = rebuildShuffledIndices(currentIndex, queue.size)
        }
    }

    fun playNext(song: Song) {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) {
            playSong(song, listOf(song))
            return
        }

        val mutableQueue = currentQueue.toMutableList()
        var insertIndex = if (currentIndex in mutableQueue.indices) currentIndex + 1 else 0
        val existingIndex = mutableQueue.indexOfFirst { it.id == song.id }

        if (existingIndex >= 0) {
            mutableQueue.removeAt(existingIndex)
            if (existingIndex < insertIndex) {
                insertIndex -= 1
            }
        }

        val safeInsertIndex = insertIndex.coerceIn(0, mutableQueue.size)
        mutableQueue.add(safeInsertIndex, song)

        _queue.value = mutableQueue
        if (_shuffleEnabled.value) {
            shuffledIndices = rebuildShuffledIndices(currentIndex, mutableQueue.size)
        }
        refreshNotificationForCurrentState()
    }

    private fun onSongCompleted() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _queue.value.getOrNull(currentIndex)?.let { playSong(it, _queue.value) }
            }

            RepeatMode.ALL -> skipNext()

            RepeatMode.OFF -> {
                if (currentIndex < _queue.value.lastIndex) {
                    skipNext()
                } else {
                    pause()
                }
            }
        }
    }

    private fun nextShuffledIndex(): Int {
        val songs = _queue.value
        if (songs.isEmpty()) return currentIndex.coerceAtLeast(0)

        val pool = shuffledIndices.ifEmpty { songs.indices.toList() }
        val currentPos = pool.indexOf(currentIndex)
        val nextPos = if (currentPos < 0) 0 else (currentPos + 1) % pool.size
        return pool[nextPos]
    }

    private fun rebuildShuffledIndices(current: Int, size: Int): List<Int> {
        if (size <= 0) return emptyList()
        val all = (0 until size).toList()
        if (current !in all) return all.shuffled()
        val remaining = all.filter { it != current }.shuffled()
        return listOf(current) + remaining
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
                val position = player.currentPosition.toLong().coerceAtLeast(0L)
                _playbackState.value = state.copy(position = position)
                updateMediaSessionPlaybackState(isPlaying = true, positionMs = position)
                refreshNotificationForCurrentState()
            }

            else -> Unit
        }
    }

    private fun updateMediaSessionMetadata(song: Song) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.durationMs)
            .also { builder ->
                currentAlbumArt?.let { builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) }
            }
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun updateMediaSessionPlaybackState(isPlaying: Boolean, positionMs: Long) {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, positionMs, 1.0f)
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun startPlaybackForeground(song: Song) {
        scope.launch {
            val art = withContext(Dispatchers.IO) {
                notificationHelper.loadAlbumArt(song)
            }
            currentAlbumArt = art
            updateMediaSessionMetadata(song)

            val state = _playbackState.value
            val duration = currentDuration(state).takeIf { it > 0L } ?: song.durationMs
            val position = currentPosition(state)

            if (canPostNotifications()) {
                val notification = notificationHelper.buildNotification(
                    song = song,
                    isPlaying = state is PlayerState.Playing,
                    repeatMode = _repeatMode.value,
                    shuffleEnabled = _shuffleEnabled.value,
                    positionMs = position,
                    durationMs = duration,
                    albumArt = currentAlbumArt,
                    mediaSession = mediaSession
                )
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun refreshNotificationForCurrentState() {
        if (!canPostNotifications()) return

        val state = _playbackState.value
        val song = currentSong(state) ?: return

        val duration = currentDuration(state).takeIf { it > 0L } ?: song.durationMs
        val position = currentPosition(state)

        notificationHelper.updateNotification(
            notificationHelper.buildNotification(
                song = song,
                isPlaying = state is PlayerState.Playing,
                repeatMode = _repeatMode.value,
                shuffleEnabled = _shuffleEnabled.value,
                positionMs = position,
                durationMs = duration,
                albumArt = currentAlbumArt,
                mediaSession = mediaSession
            )
        )
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
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
        _audioSessionIdFlow.value = 0
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
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
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
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Unit

            AudioManager.AUDIOFOCUS_GAIN -> {
                if (_playbackState.value is PlayerState.Paused) {
                    resume()
                }
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
        PlayerRepository.detachService(this)
        scope.cancel()
    }
}
