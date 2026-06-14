package com.vybzvault.music

import androidx.compose.runtime.Composable
import com.vybzvault.music.ui.player.MiniPlayer

@Deprecated(
    message = "Use MiniPlayer from com.vybzvault.music.ui.player instead.",
    replaceWith = ReplaceWith(
        expression = "MiniPlayer(playbackState, onTogglePlayPause, onSkipNext, onOpenNowPlaying)",
        imports = ["com.vybzvault.music.ui.player.MiniPlayer"]
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