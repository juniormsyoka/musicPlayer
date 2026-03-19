package com.example.music

import androidx.compose.runtime.Composable
import com.example.music.ui.player.MiniPlayer

@Deprecated(
    message = "Use MiniPlayer from com.example.music.ui.player instead.",
    replaceWith = ReplaceWith(
        expression = "MiniPlayer(playbackState, onTogglePlayPause, onSkipNext, onOpenNowPlaying)",
        imports = ["com.example.music.ui.player.MiniPlayer"]
    )
)
@Composable
fun NowPlayingBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onSetRepeatMode: (RepeatMode) -> Unit,
    formatTime: (Long) -> String,
    onOpenNowPlaying: () -> Unit = {}
) {
    MiniPlayer(
        playbackState = playbackState,
        onTogglePlayPause = onTogglePlayPause,
        onSkipNext = onSkipNext,
        onOpenNowPlaying = onOpenNowPlaying
    )
}