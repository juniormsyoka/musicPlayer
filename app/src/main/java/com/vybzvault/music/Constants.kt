package com.vybzvault.music

object PlayerConstants {
    const val SKIP_THRESHOLD_DP = 120f
    const val PROGRESS_UPDATE_INTERVAL_MS = 400L
    const val MIN_TOUCH_TARGET_DP = 48
    const val ALBUM_ART_SIZE_DP = 56
    const val NOW_PLAYING_ART_HEIGHT_DP = 340
    const val MAX_RECENT_SEARCHES = 10
    const val MAX_RECENT_SONGS = 50
    const val SLEEP_TIMER_MAX_MINUTES = 120
    const val SEEK_BACK_THRESHOLD_MS = 3000L
    const val SEARCH_DEBOUNCE_MS = 300L
    const val IMAGE_CACHE_SIZE_MB = 50L
}

object PrefKeys {
    const val FAVORITES = "favorites"
    const val RECENTS = "recents"
    const val PLAYLISTS = "playlists"
    const val PLAY_COUNTS = "play_counts"
    const val RECENT_SEARCHES = "recent_searches"
    const val QUEUE = "playback_queue"
    const val REPEAT_MODE = "repeat_mode"
    const val SHUFFLE_ENABLED = "shuffle_enabled"
    const val LIBRARY_SORT = "library_sort"
    const val SONG_TRANSITIONS = "song_transitions"
    const val FIRST_PLAYED_DAY_KEYS = "first_played_day_keys"
}