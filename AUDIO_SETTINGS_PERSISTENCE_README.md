# Audio Settings + Playback Persistence

This implementation adds:

- Playback snapshot save/restore via `StateManager`
- Dedicated `Settings` tab with audio quality controls
- Equalizer entry from `Settings` and `Now Playing`
- Parametric EQ scaffold (5 bands: frequency, gain, Q)
- Persisted equalizer + parametric settings

## What is persisted

Playback (`StateManager`):

- last played song id
- queue song ids
- queue index
- playback position
- shuffle/repeat
- volume
- playback active flag + timestamp

Equalizer (`EqualizerStore`):

- enabled state
- selected graphic preset
- 10-band graphic levels
- preamp value
- parametric enabled state
- 5 parametric bands (frequency/gain/Q)

Audio settings (`AudioSettingsStore`):

- high-res toggle
- bit-perfect toggle
- sample-rate preference
- reverb selection
- crossfade seconds
- volume normalization toggle

## Restore behavior

- Snapshot age <= 24 hours: auto-restore playback state
- Snapshot age > 24 hours: show resume banner in app
- Missing songs/files: restore gracefully with available songs, or clear snapshot if none remain

## Quick verification

1. Play a song, modify queue, seek, set EQ/preset/parametric values.
2. Put app in background (triggers save on `onStop`).
3. Relaunch app.
4. Verify restore and stale prompt behavior by adjusting device/app clock or waiting >24h.

## Local checks

```powershell
Set-Location "C:\Users\dabs\AndroidStudioProjects\Music"
.\gradlew.bat :app:compileDebugKotlin --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```
