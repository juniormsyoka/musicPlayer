package com.vybzvault.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vybzvault.music.equalizer.EqPreset
import com.vybzvault.music.equalizer.EqualizerDefaults
import com.vybzvault.music.equalizer.EqualizerEngine
import com.vybzvault.music.equalizer.EqualizerStore
import com.vybzvault.music.equalizer.PersistedEqualizerConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaybackViewModel(
    private val context: Context,
    @Suppress("unused") private val libraryStore: LibraryStore
) : ViewModel() {

    companion object {
        private const val TAG = "PlaybackViewModel"
    }

    private var musicService: MusicService? = null
    private var isBound = false
    private var pendingPlayRequest: Pair<Song, List<Song>>? = null
    private var pendingPlayNextSong: Song? = null
    private var pendingRestoreRequest: RestoreRequest? = null
    private var hasTriedRestore = false
    private var playbackStateCollectorJob: Job? = null
    private var audioSessionCollectorJob: Job? = null
    private var sleepJob: Job? = null
    private var lastTrackedPlayingSongId: Long? = null

    private data class RestoreRequest(
        val song: Song,
        val queue: List<Song>,
        val positionMs: Long,
        val shouldPlay: Boolean
    )

    private val equalizerStore = EqualizerStore(context.applicationContext)
    private val equalizerEngine = EqualizerEngine()

    val state: StateFlow<PlaybackState> = PlayerRepository.playbackState

    private val persistedEq = equalizerStore.load()
    private val _equalizerState = MutableStateFlow(
        EqualizerState(
            levels = persistedEq.bandLevelsMilliBel,
            isEnabled = persistedEq.enabled,
            selectedPreset = persistedEq.selectedPreset,
            preampDb = persistedEq.preampDb
        )
    )
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _sleepTimer = MutableStateFlow(SleepTimerState())
    val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicService.MusicBinder ?: run {
                Log.e(TAG, "MusicService binder mismatch")
                return
            }

            val serviceInstance = binder.getService()
            musicService = serviceInstance
            isBound = true
            PlayerRepository.attachService(serviceInstance)
            startAudioSessionCollector()
            syncEqualizerToNative()

            pendingRestoreRequest?.let { request ->
                serviceInstance.restorePlayback(
                    song = request.song,
                    queue = request.queue,
                    positionMs = request.positionMs,
                    shouldPlay = request.shouldPlay
                )
                pendingRestoreRequest = null
            }

            pendingPlayRequest?.let { (song, queue) ->
                serviceInstance.playSong(song, queue)
                pendingPlayRequest = null
            }

            pendingPlayNextSong?.let { song ->
                serviceInstance.playNext(song)
                pendingPlayNextSong = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            stopAudioSessionCollector()
            musicService = null
            isBound = false
            equalizerEngine.detach()
            _equalizerState.update { it.copy(audioSessionId = 0, supportedBandCount = 0, error = null) }
            PlayerRepository.detachService()
        }
    }

    init {
        bindService()
        startPlaybackStateCollector()
    }

    private fun bindService() {
        val intent = Intent(context, MusicService::class.java)
        context.startService(intent)
        val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            Log.e(TAG, "Failed to bind MusicService; playback actions will be ignored.")
        }
    }

    fun playSong(song: Song, allSongs: List<Song>) {
        val queue = if (allSongs.isEmpty()) listOf(song) else allSongs
        pendingRestoreRequest = null
        if (!PlayerRepository.playSong(song, queue)) {
            if (!isBound) bindService()
            pendingPlayRequest = song to queue
        } else {
            pendingPlayRequest = null
        }
    }

    fun savePlaybackStateSnapshot() {
        val currentState = state.value
        val currentPlayerState = currentState.playerState
        val activeSong = currentSong(currentPlayerState) ?: return

        val queue = currentState.queue.ifEmpty { listOf(activeSong) }
        val queueIndex = currentState.currentIndex
            .takeIf { it in queue.indices }
            ?: queue.indexOfFirst { it.id == activeSong.id }.coerceAtLeast(0)

        val snapshot = SavedPlaybackState(
            lastPlayedSongId = activeSong.id,
            queueSongIds = queue.map { it.id },
            queueIndex = queueIndex,
            playbackPositionMs = currentPosition(currentPlayerState).coerceAtLeast(0L),
            shuffleEnabled = currentState.shuffleEnabled,
            repeatMode = currentState.repeatMode,
            wasPlaying = currentPlayerState is PlayerState.Playing,
            savedAtEpochMs = System.currentTimeMillis()
        )

        viewModelScope.launch {
            StateManager.savePlaybackState(snapshot)
        }
    }

    fun tryRestorePlayback(songs: List<Song>) {
        if (hasTriedRestore || songs.isEmpty()) return
        hasTriedRestore = true

        viewModelScope.launch {
            val saved = StateManager.restorePlaybackState() ?: return@launch
            restoreFromSavedState(songs = songs, saved = saved, forcePlay = false)
        }
    }

    fun resumeLastSession(songs: List<Song>) {
        if (songs.isEmpty()) return

        viewModelScope.launch {
            val saved = StateManager.restorePlaybackState() ?: return@launch
            restoreFromSavedState(songs = songs, saved = saved, forcePlay = true)
        }
    }

    private fun restoreFromSavedState(
        songs: List<Song>,
        saved: SavedPlaybackState,
        forcePlay: Boolean
    ) {
        val byId = songs.associateBy { it.id }

        val restoredQueue = saved.queueSongIds
            .mapNotNull(byId::get)
            .ifEmpty { listOfNotNull(byId[saved.lastPlayedSongId]) }

        if (restoredQueue.isEmpty()) {
            viewModelScope.launch { StateManager.clearPlaybackState() }
            return
        }

        val targetSong = byId[saved.lastPlayedSongId]
            ?: restoredQueue.getOrNull(saved.queueIndex.coerceIn(0, restoredQueue.lastIndex))
            ?: restoredQueue.first()

        val resolvedIndex = restoredQueue.indexOfFirst { it.id == targetSong.id }
            .takeIf { it >= 0 }
            ?: 0

        if (state.value.shuffleEnabled != saved.shuffleEnabled) {
            PlayerRepository.setShuffle(saved.shuffleEnabled)
        }

        setRepeatMode(saved.repeatMode)

        val restoreRequest = RestoreRequest(
            song = targetSong,
            queue = restoredQueue,
            positionMs = saved.playbackPositionMs,
            shouldPlay = forcePlay || saved.wasPlaying
        )

        if (!PlayerRepository.restorePlayback(
                song = restoreRequest.song,
                queue = restoreRequest.queue,
                positionMs = restoreRequest.positionMs,
                shouldPlay = restoreRequest.shouldPlay
            )) {
            pendingPlayRequest = null
            pendingRestoreRequest = restoreRequest
            if (!isBound) bindService()
        } else {
            pendingRestoreRequest = null
        }
    }

    fun togglePlayPause() {
        when (state.value.playerState) {
            is PlayerState.Playing -> PlayerRepository.pause()
            is PlayerState.Paused -> PlayerRepository.resume()
            else -> Unit
        }
    }

    fun skipNext() {
        PlayerRepository.skipNext()
    }

    fun skipPrevious() {
        PlayerRepository.skipPrevious()
    }

    fun seekTo(positionMs: Long) {
        PlayerRepository.seekTo(positionMs)
    }

    fun setRepeatMode(mode: RepeatMode) {
        PlayerRepository.setRepeatMode(mode)
    }

    fun cycleRepeatMode() {
        setRepeatMode(nextRepeatMode(state.value.repeatMode))
    }

    fun toggleShuffle() {
        PlayerRepository.setShuffle(!state.value.shuffleEnabled)
    }

    fun updateQueue(queue: List<Song>) {
        PlayerRepository.updateQueue(queue)
    }

    fun playNext(song: Song) {
        pendingRestoreRequest = null
        if (!PlayerRepository.playNext(song)) {
            if (!isBound) bindService()
            pendingPlayNextSong = song
        } else {
            pendingPlayNextSong = null
        }
    }

    private fun startAudioSessionCollector() {
        stopAudioSessionCollector()
        audioSessionCollectorJob = viewModelScope.launch {
            PlayerRepository.audioSessionId.collect { audioSessionId ->
                handleAudioSessionChanged(audioSessionId)
            }
        }
    }

    private fun startPlaybackStateCollector() {
        stopPlaybackStateCollector()
        playbackStateCollectorJob = viewModelScope.launch {
            state.collect { playbackState ->
                val playerState = playbackState.playerState
                if (playerState is PlayerState.Playing) {
                    val songId = playerState.song.id
                    if (lastTrackedPlayingSongId != songId) {
                        libraryStore.recordPlayEvent(
                            songId = songId,
                            previousSongId = lastTrackedPlayingSongId
                        )
                        lastTrackedPlayingSongId = songId
                    }
                }
            }
        }
    }

    private fun stopAudioSessionCollector() {
        audioSessionCollectorJob?.cancel()
        audioSessionCollectorJob = null
    }

    private fun stopPlaybackStateCollector() {
        playbackStateCollectorJob?.cancel()
        playbackStateCollectorJob = null
    }

    private fun handleAudioSessionChanged(audioSessionId: Int) {
        if (audioSessionId <= 0) {
            equalizerEngine.detach()
            _equalizerState.update { it.copy(audioSessionId = 0, supportedBandCount = 0, error = null) }
            return
        }

        val capabilities = equalizerEngine.attach(audioSessionId)
        if (!capabilities.isSupported) {
            _equalizerState.update {
                it.copy(
                    audioSessionId = audioSessionId,
                    supportedBandCount = 0,
                    error = "Equalizer not supported on this device"
                )
            }
            return
        }

        _equalizerState.update { state ->
            val seededLevels = if (state.levels.isEmpty()) {
                equalizerEngine.readVirtualBandLevels()
            } else {
                state.levels
            }

            state.copy(
                levels = seededLevels,
                range = capabilities.bandRange.first to capabilities.bandRange.last,
                audioSessionId = audioSessionId,
                supportedBandCount = capabilities.physicalBandCount,
                error = null
            )
        }
        syncEqualizerToNative()
    }

    private fun syncEqualizerToNative() {
        val state = _equalizerState.value
        equalizerEngine.apply(state.isEnabled, state.levels, state.preampDb)
    }

    private fun persistEqualizerState() {
        val state = _equalizerState.value
        equalizerStore.save(
            PersistedEqualizerConfig(
                enabled = state.isEnabled,
                selectedPreset = state.selectedPreset,
                preampDb = state.preampDb,
                bandLevelsMilliBel = state.levels
            )
        )
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.update { it.copy(isEnabled = enabled) }
        syncEqualizerToNative()
        persistEqualizerState()
    }

    fun setPreset(preset: EqPreset) {
        val range = _equalizerState.value.range.let { it.first..it.second }
        val presetLevels = if (preset == EqPreset.CUSTOM) {
            _equalizerState.value.levels
        } else {
            EqualizerDefaults.presetLevelsMilliBel(preset, range)
        }

        _equalizerState.update { it.copy(selectedPreset = preset, levels = presetLevels) }
        syncEqualizerToNative()
        persistEqualizerState()
    }

    fun setPreamp(preampDb: Float) {
        val clamped = preampDb.coerceIn(EqualizerDefaults.PREAMP_MIN_DB, EqualizerDefaults.PREAMP_MAX_DB)
        _equalizerState.update { it.copy(preampDb = clamped) }
        syncEqualizerToNative()
        persistEqualizerState()
    }

    fun setBandLevel(band: Int, level: Int) {
        val range = _equalizerState.value.range
        val clampedLevel = level.coerceIn(range.first, range.second)

        _equalizerState.update { state ->
            val levels = state.levels.ifEmpty { List(EqualizerDefaults.BAND_FREQUENCIES_HZ.size) { 0 } }
            val updated = levels.toMutableList()
            val safeBand = band.coerceIn(0, updated.lastIndex)
            updated[safeBand] = clampedLevel
            state.copy(levels = updated, selectedPreset = EqPreset.CUSTOM)
        }

        syncEqualizerToNative()
        persistEqualizerState()
    }

    fun setBandLevel(band: Short, level: Int) {
        setBandLevel(band.toInt(), level)
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _sleepTimer.value = SleepTimerState()
            return
        }

        val durationMs = minutes * 60_000L
        val targetTime = System.currentTimeMillis() + durationMs
        _sleepTimer.value = SleepTimerState(targetTimeMs = targetTime, remainingMs = durationMs, isActive = true)

        sleepJob = viewModelScope.launch {
            while (_sleepTimer.value.isActive) {
                val remaining = (targetTime - System.currentTimeMillis()).coerceAtLeast(0L)
                if (remaining == 0L) {
                    musicService?.pause()
                    _sleepTimer.value = SleepTimerState()
                    break
                }
                _sleepTimer.value = _sleepTimer.value.copy(remainingMs = remaining)
                delay(1000L)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        _sleepTimer.value = SleepTimerState()
    }

    fun formatTime(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0L)
        val minutes = total / 60L
        val seconds = total % 60L
        return "%d:%02d".format(minutes, seconds)
    }


    override fun onCleared() {
        super.onCleared()
        stopPlaybackStateCollector()
        stopAudioSessionCollector()
        sleepJob?.cancel()

        if (isBound) {
            runCatching { context.unbindService(connection) }
            isBound = false
        }

        equalizerEngine.detach()
    }
}