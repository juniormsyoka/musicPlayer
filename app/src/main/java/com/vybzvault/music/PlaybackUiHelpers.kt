package com.vybzvault.music

internal fun currentSong(playerState: PlayerState): Song? = when (playerState) {
    is PlayerState.Ready -> playerState.song
    is PlayerState.Playing -> playerState.song
    is PlayerState.Paused -> playerState.song
    is PlayerState.Error -> playerState.song
    PlayerState.Idle,
    PlayerState.Preparing -> null
}

internal fun currentPosition(playerState: PlayerState): Long = when (playerState) {
    is PlayerState.Playing -> playerState.position
    is PlayerState.Paused -> playerState.position
    else -> 0L
}

internal fun currentDuration(playerState: PlayerState): Long = when (playerState) {
    is PlayerState.Ready -> playerState.duration
    is PlayerState.Playing -> playerState.duration
    is PlayerState.Paused -> playerState.duration
    else -> 0L
}

internal fun nextRepeatMode(current: RepeatMode): RepeatMode = when (current) {
    RepeatMode.OFF -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.OFF
}

