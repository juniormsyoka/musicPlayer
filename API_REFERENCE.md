# Audio Settings & Persistence - API Reference

## StateManager (Playback Persistence)

**Location**: `com.example.music.StateManager`

### Initialization
```kotlin
// Call in MainActivity.onCreate() ONCE
StateManager.initialize(applicationContext)
```

### Save Playback State
```kotlin
// Called automatically by MainActivity lifecycle observer on onStop
playbackViewModel.savePlaybackStateSnapshot()
```

### Restore Playback State
```kotlin
// Called automatically when library loads in MainActivity
playbackViewModel.tryRestorePlayback(songs: List<Song>)
```

### Resume Deferred Playback
```kotlin
// Called when user taps "Resume" on >24h stale snapshot banner
playbackViewModel.resumeDeferredPlayback()

// Called when user taps "Dismiss"
playbackViewModel.dismissResumePrompt()
```

### Data Structure
```kotlin
data class SavedPlaybackState(
    val lastPlayedSongId: Long,
    val queueSongIds: List<Long>,
    val queueIndex: Int,
    val playbackPositionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
    val wasPlaying: Boolean,
    val savedAtEpochMs: Long
)
```

---

## EqualizerScreen

**Location**: `com.example.music.ui.screens.EqualizerScreen`

### Composition
```kotlin
@Composable
fun EqualizerScreen(
    equalizerState: EqualizerState,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSetPreset: (EqPreset) -> Unit,
    onSetPreamp: (Float) -> Unit,
    onSetBandLevel: (index: Int, levelMilliBel: Int) -> Unit,
    onToggleParametricEnabled: (Boolean) -> Unit,
    onSetParametricBandFrequency: (index: Int, frequencyHz: Int) -> Unit,
    onSetParametricBandGain: (index: Int, gainDb: Float) -> Unit,
    onSetParametricBandQ: (index: Int, q: Float) -> Unit
)
```

### Presets Available
```kotlin
enum class EqPreset {
    ROCK,        // Strong bass/treble
    POP,         // Mid-high presence
    JAZZ,        // Balanced, warm
    CLASSICAL,   // Flat, minimal boost
    BASS_BOOST,  // Heavy low-end
    CUSTOM       // User-edited
}
```

### 10-Band Frequencies
```kotlin
listOf(32, 64, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000) // Hz
```

### Parametric Band Defaults
```kotlin
listOf(
    ParametricBandConfig(80, 0f, 1.0f),      // Band 1: 80Hz, 0dB, Q=1.0
    ParametricBandConfig(250, 0f, 1.0f),     // Band 2: 250Hz
    ParametricBandConfig(1_000, 0f, 1.0f),   // Band 3: 1kHz
    ParametricBandConfig(4_000, 0f, 1.0f),   // Band 4: 4kHz
    ParametricBandConfig(10_000, 0f, 1.0f)   // Band 5: 10kHz
)
```

---

## PlaybackViewModel (Equalizer & Audio Methods)

**Location**: `com.example.music.PlaybackViewModel`

### Graphic EQ Control
```kotlin
// Enable/disable EQ
setEqualizerEnabled(enabled: Boolean)

// Select preset
setEqualizerPreset(preset: EqPreset)

// Adjust preamp (-12dB to +12dB)
setEqualizerPreamp(preampDb: Float)

// Adjust individual band (in milliBels)
// Range depends on device; typically -1500 to +1500 mB (-15dB to +15dB)
setEqualizerBandLevel(index: Int, levelMilliBel: Int)
```

### Parametric EQ Control
```kotlin
// Enable/disable parametric mode
setParametricEnabled(enabled: Boolean)

// Set frequency (20Hz to 20kHz)
setParametricBandFrequency(index: Int, frequencyHz: Int)

// Set gain (-12dB to +12dB)
setParametricBandGain(index: Int, gainDb: Float)

// Set Q (0.3 to 10)
setParametricBandQ(index: Int, q: Float)
```

### Audio Settings Control
```kotlin
// Update audio settings (uses reducer pattern)
updateAudioSettings { current ->
    current.copy(
        highResPlaybackEnabled = true,
        bitPerfectEnabled = false,
        sampleRatePreference = SampleRatePreference.HZ_96,
        reverbPreset = ReverbPreset.HALL,
        crossfadeSeconds = 2,
        volumeNormalizationEnabled = true
    )
}

// Or individual helpers:
onSetHighResEnabled(enabled: Boolean)
onSetBitPerfectEnabled(enabled: Boolean)
onSetSampleRatePreference(pref: SampleRatePreference)
onSetReverbPreset(preset: ReverbPreset)
onSetCrossfadeSeconds(seconds: Int)
onSetVolumeNormalizationEnabled(enabled: Boolean)
```

### State Flows
```kotlin
val equalizerState: StateFlow<EqualizerState>     // Graphic + parametric EQ state
val audioSettings: StateFlow<AudioSettingsState>  // Audio quality settings
val resumePromptState: StateFlow<ResumePromptState> // Stale snapshot prompt visibility
```

---

## SettingsScreen

**Location**: `com.example.music.ui.screens.SettingsScreen`

### Composition
```kotlin
@Composable
fun SettingsScreen(
    audioSettingsState: AudioSettingsState,
    onOpenEqualizer: () -> Unit,
    onSetHighResEnabled: (Boolean) -> Unit,
    onSetBitPerfectEnabled: (Boolean) -> Unit,
    onSetSampleRatePreference: (SampleRatePreference) -> Unit,
    onSetReverbPreset: (ReverbPreset) -> Unit,
    onSetCrossfadeSeconds: (Int) -> Unit,
    onSetVolumeNormalizationEnabled: (Boolean) -> Unit
)
```

### Sample Rate Options
```kotlin
enum class SampleRatePreference {
    AUTO,    // Device default
    HZ_44_1, // 44.1 kHz
    HZ_48,   // 48 kHz
    HZ_96,   // 96 kHz
    HZ_192   // 192 kHz
}
```

### Reverb Options
```kotlin
enum class ReverbPreset {
    OFF,    // No effect
    ROOM,   // Small room
    HALL,   // Concert hall
    PLATE   // Plate reverb
}
```

---

## Persistence Stores

### EqualizerStore
```kotlin
class EqualizerStore(context: Context) {
    fun load(): PersistedEqualizerConfig
    fun save(config: PersistedEqualizerConfig)
}

data class PersistedEqualizerConfig(
    val enabled: Boolean,
    val selectedPreset: EqPreset,
    val preampDb: Float,
    val bandLevelsMilliBel: List<Int>,
    val parametricEnabled: Boolean,
    val parametricBands: List<ParametricBandConfig>
)
```

### AudioSettingsStore
```kotlin
class AudioSettingsStore(context: Context) {
    fun load(): AudioSettingsState
    fun save(value: AudioSettingsState)
}
```

### StateManager
```kotlin
object StateManager {
    fun initialize(context: Context)
    suspend fun savePlaybackState(state: SavedPlaybackState)
    suspend fun restorePlaybackState(): SavedPlaybackState?
    suspend fun clearPlaybackState()
}
```

---

## State Data Classes

### EqualizerState
```kotlin
data class EqualizerState(
    val levels: List<Int> = List(10) { 0 },  // milliBels
    val bandFrequenciesHz: List<Int> = BAND_FREQUENCIES_HZ,
    val range: Pair<Int, Int> = -1500 to 1500,
    val preampDb: Float = 0f,
    val selectedPreset: EqPreset = EqPreset.CUSTOM,
    val isEnabled: Boolean = false,
    val parametricEnabled: Boolean = false,
    val parametricBands: List<ParametricBandConfig> = PARAMETRIC_BANDS_DEFAULT,
    val supportedBandCount: Int = 0,
    val audioSessionId: Int = -1,
    val error: String? = null
)
```

### AudioSettingsState
```kotlin
data class AudioSettingsState(
    val highResPlaybackEnabled: Boolean = false,
    val bitPerfectEnabled: Boolean = false,
    val sampleRatePreference: SampleRatePreference = SampleRatePreference.AUTO,
    val reverbPreset: ReverbPreset = ReverbPreset.OFF,
    val crossfadeSeconds: Int = 0,
    val volumeNormalizationEnabled: Boolean = false
)
```

### ResumePromptState
```kotlin
data class ResumePromptState(
    val visible: Boolean = false,
    val songTitle: String = "",
    val savedAtEpochMs: Long = 0L
)
```

---

## Unit Test Utilities

### Run tests
```bash
./gradlew :app:testDebugUnitTest
```

### EqualizerDefaultsTest
```kotlin
class EqualizerDefaultsTest {
    fun presetLevels_returnsTenBands()        // Verify 10-band output
    fun presetLevels_areClampedToRange()     // Verify range bounds
}
```

---

## Common Usage Patterns

### Open Equalizer from Settings
```kotlin
// In SettingsScreen composable
Button(onClick = onOpenEqualizer) {
    Text("Open Equalizer")
}

// In MusicAppContent, showEqualizerScreen controls modal
if (showEqualizerScreen) {
    EqualizerScreen(...)
}
```

### Open Equalizer from Now Playing
```kotlin
// In NowPlayingScreen top bar
IconButton(onClick = onOpenEqualizer) {
    Icon(Icons.Default.Equalizer, contentDescription = "Equalizer")
}
```

### Respond to Resume Prompt
```kotlin
if (resumePromptState.visible) {
    Card {
        Text("Resume playback from ${resumePromptState.songTitle}?")
        Button(onClick = onResumeDeferredPlayback) { Text("Resume") }
        Button(onClick = onDismissResumePrompt) { Text("Dismiss") }
    }
}
```

### Save state on app backgrounding
```kotlin
// In MainActivity.onCreate()
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_STOP) {
            playbackViewModel.savePlaybackStateSnapshot()
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

---

**Last Updated**: March 24, 2026

