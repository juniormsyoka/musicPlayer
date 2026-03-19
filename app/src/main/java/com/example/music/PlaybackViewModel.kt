package com.example.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaybackViewModel(
    private val context: Context,
    private val libraryStore: LibraryStore
) : ViewModel() {

    companion object {
        private const val TAG = "PlaybackViewModel"
    }

    private var musicService: MusicService? = null
    private var isBound = false
    private var pendingPlayRequest: Pair<Song, List<Song>>? = null
    private var serviceCollectorJob: Job? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _sleepTimer = MutableStateFlow(SleepTimerState())
    val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()

    private var equalizer: Equalizer? = null
    private val _volumePercent = MutableStateFlow(readCurrentVolumePercent())
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private var sleepJob: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicService.MusicBinder
            if (binder == null) {
                Log.e(TAG, "Unexpected binder type from MusicService")
                return
            }

            val serviceInstance = binder.getService()
            musicService = serviceInstance
            isBound = true

            pendingPlayRequest?.let { (song, queue) ->
                musicService?.playSong(song, queue)
                pendingPlayRequest = null
            }

            startServiceCollectors(serviceInstance)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            stopServiceCollectors()
            musicService = null
            isBound = false
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        val intent = Intent(context, MusicService::class.java)
        val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            Log.e(TAG, "Failed to bind MusicService; playback actions will be ignored.")
        }
        context.startService(intent)
    }

    fun playSong(song: Song, allSongs: List<Song>) {
        val service = musicService
        if (service != null) {
            val queue = if (allSongs.isEmpty()) listOf(song) else allSongs
            service.playSong(song, queue)
            return
        }

        pendingPlayRequest = song to (if (allSongs.isEmpty()) listOf(song) else allSongs)
        if (!isBound) {
            bindService()
        }
    }

    fun togglePlayPause() {
        when (_state.value.playerState) {
            is PlayerState.Playing -> musicService?.pause()
            is PlayerState.Paused  -> musicService?.resume()
            else -> {}
        }
    }

    fun pause() {
        musicService?.pause()
    }

    fun skipNext() {
        musicService?.skipNext()
    }

    fun skipPrevious() {
        musicService?.skipPrevious()
    }

    fun seekTo(positionMs: Long) {
        musicService?.seekTo(positionMs)
    }

    fun setRepeatMode(mode: RepeatMode) {
        musicService?.setRepeatMode(mode)
        _state.update { it.copy(repeatMode = mode) }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    fun toggleShuffle() {
        val newValue = !_state.value.shuffleEnabled
        musicService?.setShuffle(newValue)
        _state.update { it.copy(shuffleEnabled = newValue) }
    }

    fun updateQueue(queue: List<Song>) {
        musicService?.updateQueue(queue)
        _state.update { it.copy(queue = queue) }
        syncCurrentIndex()
    }

    private fun startServiceCollectors(service: MusicService) {
        stopServiceCollectors()
        serviceCollectorJob = viewModelScope.launch {
            launch {
                service.playbackState.collect { playerState ->
                    _state.update { it.copy(playerState = playerState) }
                    syncCurrentIndex()

                    if (playerState is PlayerState.Playing) {
                        libraryStore.addRecent(playerState.song.id)
                        libraryStore.incrementPlayCount(playerState.song.id)
                    }
                }
            }

            launch {
                service.queue.collect { queue ->
                    _state.update { it.copy(queue = queue) }
                    syncCurrentIndex()
                }
            }

            launch {
                service.repeatModeFlow.collect { repeatMode ->
                    _state.update { it.copy(repeatMode = repeatMode) }
                }
            }

            launch {
                service.shuffleEnabledFlow.collect { isShuffleEnabled ->
                    _state.update { it.copy(shuffleEnabled = isShuffleEnabled) }
                }
            }
        }
    }

    private fun stopServiceCollectors() {
        serviceCollectorJob?.cancel()
        serviceCollectorJob = null
    }

    private fun syncCurrentIndex() {
        val nowPlayingId = currentSong(_state.value.playerState)?.id
        val index = if (nowPlayingId == null) -1 else _state.value.queue.indexOfFirst { it.id == nowPlayingId }
        _state.update { it.copy(currentIndex = index) }
    }

    fun setupEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                val range = bandLevelRange
                val levels = (0 until numberOfBands).map { getBandLevel(it.toShort()).toInt() }

                _equalizerState.update {
                    it.copy(
                        levels = levels,
                        range = range[0].toInt() to range[1].toInt(),
                        isEnabled = true
                    )
                }
            }
        } catch (_: Exception) {
            _equalizerState.update {
                it.copy(
                    isEnabled = false,
                    error = "Equalizer not supported"
                )
            }
        }
    }

    fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = ((clamped / 100f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        _volumePercent.value = clamped
    }

    fun setBandLevel(band: Short, level: Int) {
        equalizer?.setBandLevel(band, level.toShort())
        equalizer?.let { eq ->
            val levels = (0 until eq.numberOfBands).map {
                eq.getBandLevel(it.toShort()).toInt()
            }
            _equalizerState.update { it.copy(levels = levels) }
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()

        if (minutes <= 0) {
            _sleepTimer.update { SleepTimerState() }
            return
        }

        val targetTime = System.currentTimeMillis() + minutes * 60_000L
        _sleepTimer.update {
            it.copy(
                targetTimeMs = targetTime,
                isActive = true,
                remainingMs = minutes * 60_000L
            )
        }

        sleepJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            musicService?.pause()
            _sleepTimer.update { SleepTimerState() }
        }

        viewModelScope.launch {
            while (_sleepTimer.value.isActive) {
                val remaining = (_sleepTimer.value.targetTimeMs ?: 0) - System.currentTimeMillis()
                if (remaining <= 0) break
                _sleepTimer.update { it.copy(remainingMs = remaining) }
                delay(1000L)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        _sleepTimer.update { SleepTimerState() }
    }

    fun formatTime(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0L)
        val minutes = total / 60L
        val seconds = total % 60L
        return "%d:%02d".format(minutes, seconds)
    }

    private fun readCurrentVolumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((current.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    override fun onCleared() {
        super.onCleared()
        stopServiceCollectors()

        if (isBound) {
            runCatching { context.unbindService(connection) }
            isBound = false
        }

        equalizer?.release()
        equalizer = null
        sleepJob?.cancel()
    }
}