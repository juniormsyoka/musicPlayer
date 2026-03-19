# Classic Local Music Player

A local Android music player (Jetpack Compose) with a UI-first redesign: visual library browsing, full now-playing screen, playlists, search, and settings.

## Features

- Library/Home with large album-art cards, recently added, quick-play actions, pull-to-refresh, and skeleton loading.
- Full-screen Now Playing with seek position, swipe-to-skip gesture, pinch-to-zoom art, shuffle/repeat, and queue panel.
- Playlists screen with smart playlists (most played/recently added), visual cards, and long-press drag reordering.
- Search screen with song/artist/album filters, recent searches, and popularity hints.
- Context menus and multi-select action bar for playlist/favorite bulk actions.
- Mini player inside a `BottomSheetScaffold` plus haptic feedback on key actions.
- Equalizer moved to Settings (instead of always visible on Now Playing).

## Project Structure

- `app/src/main/java/com/example/music/MainActivity.kt`: Main Compose app shell + all redesigned screens.
- `app/src/main/java/com/example/music/MusicViewModel.kt`: Playback engine, queue, repeat/shuffle, selection, search/filter state.
- `app/src/main/java/com/example/music/MusicRepository.kt`: Local audio query via `MediaStore`.
- `app/src/main/java/com/example/music/LibraryStore.kt`: Persistent favorites, playlists, recents, play counts, recent searches.
- `app/src/main/java/com/example/music/Song.kt`: Song/playlist models.

## Compose Compatibility Note

- No `FlowRow` is used in the redesigned UI to avoid `NoSuchMethodError` crashes from mixed Compose runtime/foundation versions.

## Permissions

The app requests audio library access at runtime:

- `READ_MEDIA_AUDIO` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12 and below)

## Build and Test

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Run

```powershell
.\gradlew.bat :app:installDebug
```

Then launch **Classic Local Music Player** on your device/emulator and grant media permission.

