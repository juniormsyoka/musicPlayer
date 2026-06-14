package com.vybzvault.music

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

object PlayerRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    @Volatile
    private var service: MusicService? = null
    private var syncJob: Job? = null
    private var audioSessionJob: Job? = null

    fun attachService(service: MusicService) {
        if (this.service === service) return
        this.service = service
        syncJob?.cancel()
        audioSessionJob?.cancel()

        _playbackState.value = buildPlaybackState(
            playerState = service.playbackState.value,
            queue = service.queue.value,
            repeatMode = service.repeatModeFlow.value,
            shuffleEnabled = service.shuffleEnabledFlow.value
        )
        _audioSessionId.value = service.currentAudioSessionId()

        syncJob = scope.launch {
            combine(
                service.playbackState,
                service.queue,
                service.repeatModeFlow,
                service.shuffleEnabledFlow
            ) { playerState, queue, repeatMode, shuffleEnabled ->
                buildPlaybackState(
                    playerState = playerState,
                    queue = queue,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled
                )
            }.collectLatest { playbackState ->
                _playbackState.value = playbackState
            }
        }

        audioSessionJob = scope.launch {
            service.audioSessionIdFlow.collectLatest { sessionId ->
                _audioSessionId.value = sessionId
            }
        }
    }

    fun detachService(service: MusicService? = null) {
        if (service != null && this.service !== service) return
        this.service = null
        syncJob?.cancel()
        audioSessionJob?.cancel()
        syncJob = null
        audioSessionJob = null
        _audioSessionId.value = 0
        _playbackState.value = PlaybackState()
    }

    fun playSong(song: Song, queue: List<Song>): Boolean {
        val current = service ?: return false
        current.playSong(song, queue)
        return true
    }

    fun restorePlayback(song: Song, queue: List<Song>, positionMs: Long, shouldPlay: Boolean): Boolean {
        val current = service ?: return false
        current.restorePlayback(song, queue, positionMs, shouldPlay)
        return true
    }

    fun pause() {
        service?.pause()
    }

    fun resume() {
        service?.resume()
    }

    fun skipNext() {
        service?.skipNext()
    }

    fun skipPrevious() {
        service?.skipPrevious()
    }

    fun seekTo(positionMs: Long) {
        service?.seekTo(positionMs)
    }

    fun setRepeatMode(mode: RepeatMode) {
        service?.setRepeatMode(mode)
    }

    fun setShuffle(enabled: Boolean) {
        service?.setShuffle(enabled)
    }

    fun updateQueue(queue: List<Song>) {
        service?.updateQueue(queue)
    }

    fun playNext(song: Song): Boolean {
        val current = service ?: return false
        current.playNext(song)
        return true
    }

    private fun buildPlaybackState(
        playerState: PlayerState,
        queue: List<Song>,
        repeatMode: RepeatMode,
        shuffleEnabled: Boolean
    ): PlaybackState {
        val nowPlayingId = currentSong(playerState)?.id
        val currentIndex = queue.indexOfFirst { it.id == nowPlayingId }
        return PlaybackState(
            playerState = playerState,
            queue = queue,
            currentIndex = currentIndex,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            shuffledIndices = emptyList()
        )
    }
}


