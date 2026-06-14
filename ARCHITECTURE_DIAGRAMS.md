# Visual Architecture & Feature Summary

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          USER INTERFACE                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐            │
│  │   Home       │  │   Search     │  │   Library    │   ...      │
│  └──────────────┘  └──────────────┘  └──────────────┘            │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐            │
│  │ Now Playing  │  │ Now Playing  │  │  Settings    │            │
│  │  + EQ btn    │  │              │  │              │            │
│  └──────────────┘  └──────────────┘  └──────────────┘            │
│       [EQ icon]         [EQ icon]                                 │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │              EQUALIZER SCREEN (Modal)                   │     │
│  ├─────────────────────────────────────────────────────────┤     │
│  │ [Graphic] [Parametric] ← Tab Selection                  │     │
│  │                                                          │     │
│  │ GRAPHIC TAB:                                            │     │
│  │ ├─ Enable/Disable Toggle                               │     │
│  │ ├─ Presets (Rock, Pop, Jazz, Classical, ...)          │     │
│  │ ├─ Preamp Slider (-12dB to +12dB)                      │     │
│  │ ├─ 10 Band Sliders (32Hz - 16kHz)                      │     │
│  │ └─ Frequency Response Chart (animated)                 │     │
│  │                                                          │     │
│  │ PARAMETRIC TAB:                                         │     │
│  │ ├─ Enable/Disable Toggle                               │     │
│  │ └─ 5 Band Cards (Freq/Gain/Q sliders each)             │     │
│  │    Band 1: 80Hz                                         │     │
│  │    Band 2: 250Hz                                        │     │
│  │    Band 3: 1kHz                                         │     │
│  │    Band 4: 4kHz                                         │     │
│  │    Band 5: 10kHz                                        │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │         SETTINGS SCREEN (Tab Selection)                 │     │
│  ├─────────────────────────────────────────────────────────┤     │
│  │ ┌─ Audio Quality ─────────────────────────────────────┐ │     │
│  │ │ ☐ High-res Playback                                 │ │     │
│  │ │ ☐ Bit-perfect Mode                                  │ │     │
│  │ │ [Auto] [44.1k] [48k] [96k] [192k]                  │ │     │
│  │ └─────────────────────────────────────────────────────┘ │     │
│  │                                                          │     │
│  │ ┌─ Audio Effects ────────────────────────────────────┐  │     │
│  │ │ [Open Equalizer] ← Opens modal EQ Screen          │  │     │
│  │ │ [Off] [Room] [Hall] [Plate]                       │  │     │
│  │ │ Crossfade: ▬●▬▬▬▬ (0-12s)                         │  │     │
│  │ │ ☐ Volume Normalization                             │  │     │
│  │ └─────────────────────────────────────────────────────┘  │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │         RESUME PROMPT (Stale Snapshot >24h)            │     │
│  ├─────────────────────────────────────────────────────────┤     │
│  │ Resume playback from "Song Title"?                      │     │
│  │ [Resume]  [Dismiss]                                     │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                            ↓ ↓ ↓
┌─────────────────────────────────────────────────────────────────┐
│                    VIEW MODEL LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  PlaybackViewModel:                                              │
│  ├─ equalizerState (StateFlow<EqualizerState>)                  │
│  ├─ audioSettings (StateFlow<AudioSettingsState>)               │
│  ├─ resumePromptState (StateFlow<ResumePromptState>)            │
│  └─ Actions: set...(), save...(), restore...()                 │
│                                                                   │
│  Functions:                                                      │
│  ├─ setEqualizerEnabled(Boolean)                                │
│  ├─ setEqualizerPreset(EqPreset)                                │
│  ├─ setEqualizerBandLevel(index, mB)                            │
│  ├─ setParametricEnabled(Boolean)                               │
│  ├─ setParametricBandFrequency/Gain/Q(index, value)             │
│  ├─ updateAudioSettings(reducer)                                │
│  ├─ savePlaybackStateSnapshot()                                 │
│  ├─ tryRestorePlayback(songs)                                   │
│  └─ resumeDeferredPlayback() / dismissResumePrompt()            │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                            ↓ ↓ ↓
┌─────────────────────────────────────────────────────────────────┐
│                  PERSISTENCE & ENGINE LAYER                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌────────────────────┐  ┌────────────────────┐                 │
│  │  EqualizerStore    │  │   AudioSettingsStore│                 │
│  ├────────────────────┤  ├────────────────────┤                 │
│  │ load() / save()    │  │ load() / save()    │                 │
│  │ JSON serialization │  │ JSON serialization │                 │
│  └────────────────────┘  └────────────────────┘                 │
│           ↓                       ↓                               │
│  ┌────────────────────┐  ┌────────────────────┐                 │
│  │  EqualizerEngine   │  │   StateManager     │                 │
│  ├────────────────────┤  ├────────────────────┤                 │
│  │ attach() / apply() │  │ save() / restore() │                 │
│  │ Band mapping       │  │ Snapshot handling  │                 │
│  │ AudioEffect wrapper│  │ Age detection      │                 │
│  └────────────────────┘  └────────────────────┘                 │
│           ↓                       ↓                               │
└─────────────────────────────────────────────────────────────────┘
                            ↓ ↓ ↓
┌─────────────────────────────────────────────────────────────────┐
│           PERSISTENCE LAYER (SharedPreferences)                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  playback_state_store:       equalizer_store:                    │
│  ├─ lastPlayedSongId         ├─ eq_enabled                       │
│  ├─ queueSongIds             ├─ eq_selected_preset               │
│  ├─ queueIndex               ├─ eq_preamp_db                     │
│  ├─ playbackPositionMs       ├─ eq_band_levels                   │
│  ├─ shuffleEnabled           ├─ eq_param_enabled                 │
│  ├─ repeatMode               └─ eq_param_bands                   │
│  ├─ wasPlaying               audio_settings_store:               │
│  └─ savedAtEpochMs           ├─ high_res                         │
│                              ├─ bit_perfect                      │
│                              ├─ sample_rate                      │
│                              ├─ reverb                           │
│                              ├─ crossfade_seconds                │
│                              └─ volume_normalization             │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                            ↓ ↓ ↓
┌─────────────────────────────────────────────────────────────────┐
│         ANDROID FRAMEWORK & SYSTEM LAYER                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Android AudioEffect Equalizer (via AudioSessionId)             │
│  └─ 10-band equalization with device band mapping               │
│                                                                   │
│  MediaPlayer (in MusicService)                                  │
│  └─ Audio playback + session ID                                 │
│                                                                   │
│  Lifecycle Observers (MainActivity)                             │
│  └─ onStop → save, onCreate → restore                           │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Playback Restore Flow (Detailed)

```
APP CLOSE (onStop)
         ↓
  ┌─────────────────────────────────┐
  │ LifecycleEventObserver fires    │
  │ playbackViewModel.              │
  │ savePlaybackStateSnapshot()     │
  └─────────────────────────────────┘
         ↓
  ┌─────────────────────────────────┐
  │ Extract current state:          │
  │ • Song ID                       │
  │ • Queue (list of IDs)           │
  │ • Queue index                   │
  │ • Playback position             │
  │ • Shuffle/repeat/volume         │
  │ • Timestamp                     │
  └─────────────────────────────────┘
         ↓
  ┌─────────────────────────────────┐
  │ StateManager.savePlaybackState()│
  │ └─ JSON encode                  │
  │    └─ SharedPreferences write   │
  └─────────────────────────────────┘
         ↓
  [Device powered off, time passes]
         ↓
APP LAUNCH (onCreate)
         ↓
  ┌─────────────────────────────────┐
  │ StateManager.initialize(        │
  │   applicationContext            │
  │ )                               │
  └─────────────────────────────────┘
         ↓
  ┌─────────────────────────────────┐
  │ LibraryViewModel.refreshLibrary()
  │ └─ Load all songs from storage  │
  └─────────────────────────────────┘
         ↓
  ┌─────────────────────────────────┐
  │ Songs loaded → LaunchedEffect   │
  │ calls playbackViewModel.        │
  │ tryRestorePlayback(songs)       │
  └─────────────────────────────────┘
         ↓
  ┌─────────────────────────────────┐
  │ StateManager.restorePlayback    │
  │ State() reads from SharedPrefs  │
  └─────────────────────────────────┘
         ↓
  ┌─────────────────────────────────┐
  │ Check saved timestamp           │
  │ age = now - savedAt             │
  └─────────────────────────────────┘
         ↓
  ╔═════════════════════════════════╗
  ║  Is age ≤ 24 hours?             ║
  ╚═════════════════════════════════╝
    ↙ YES                      ↘ NO
    ↓                          ↓
  SILENT AUTO-RESTORE      SHOW STALE BANNER
  ├─ Map saved song IDs     ├─ "Resume playback
  │  to library songs       │  from [Song]?"
  ├─ Create queue          │  [Resume] [Dismiss]
  ├─ Play song             │
  ├─ Seek to position      ├─ Wait for user
  ├─ Restore shuffle/      │  interaction
  │  repeat/volume         │
  └─ (optionally pause)    └─ On [Resume]:
                              Auto-restore
                              (same as left side)
```

---

## 📊 State Machine Diagram

```
                    APP LIFECYCLE
┌──────────────────────────────────────────────────┐
│                                                  │
│  ┌──────────────┐                                │
│  │  onCreate()  │                                │
│  └──────┬───────┘                                │
│         │                                        │
│         ├─ StateManager.initialize()             │
│         ├─ Bind MusicService                     │
│         ├─ Load Library                          │
│         └─ Trigger Restore (LaunchedEffect)      │
│                                                  │
│  ┌──────────────┐      ┌──────────────┐         │
│  │  onStart()   │      │  onResume()  │         │
│  └──────┬───────┘      └──────┬───────┘         │
│         └──────────┬──────────┘                  │
│                    │                             │
│              [USER ACTIVE]                       │
│              ├─ Play/Pause                       │
│              ├─ Adjust EQ                        │
│              ├─ Change Settings                  │
│              └─ Navigate UI                      │
│                                                  │
│         ┌──────────────┐                         │
│         │  onPause()   │                         │
│         └──────┬───────┘                         │
│                │                                 │
│              [USER INACTIVE]                     │
│                │                                 │
│         ┌──────────────┐                         │
│         │  onStop()    │                         │
│         └──────┬───────┘                         │
│                │                                 │
│         Save Snapshot:                           │
│         ├─ Playback state                        │
│         ├─ EQ settings                           │
│         └─ Audio settings                        │
│                │                                 │
│         ┌──────────────┐                         │
│         │ onDestroy()  │                         │
│         └──────────────┘                         │
│                                                  │
└──────────────────────────────────────────────────┘
```

---

## 🎛️ Equalizer Control State Tree

```
EqualizerState
├─ isEnabled: Boolean
│  ├─ ON → Apply to audio
│  └─ OFF → Bypass audio
│
├─ bandFrequenciesHz: List<Int> (10 values)
│  └─ [32, 64, 125, 250, 500, 1k, 2k, 4k, 8k, 16k]
│
├─ levels: List<Int> (10 values in milliBels)
│  └─ Range: -1500 to +1500 mB (-15dB to +15dB)
│
├─ selectedPreset: EqPreset
│  ├─ ROCK → [+4, +3, +2, +1, -1, -1, +1, +2, +3, +4] dB
│  ├─ POP → [-1, +1, +3, +4, +3, +1, -1, -2, -1, 0] dB
│  ├─ JAZZ → [+2, +1, 0, +1, +2, +2, +1, 0, +1, +2] dB
│  ├─ CLASSICAL → [0, 0, 0, +1, +2, +2, +1, +1, 0, 0] dB
│  ├─ BASS_BOOST → [+6, +5, +4, +2, 0, -1, -2, -3, -3, -3] dB
│  └─ CUSTOM → User-edited (auto-selected when slider moved)
│
├─ preampDb: Float
│  └─ Range: -12dB to +12dB (applied to all bands)
│
├─ parametricEnabled: Boolean
│  └─ Enables 5-band parametric controls
│
├─ parametricBands: List<ParametricBandConfig> (5 bands)
│  ├─ [0] Frequency: 20-20000Hz, Gain: -12-+12dB, Q: 0.3-10
│  ├─ [1] Frequency: 20-20000Hz, Gain: -12-+12dB, Q: 0.3-10
│  ├─ [2] Frequency: 20-20000Hz, Gain: -12-+12dB, Q: 0.3-10
│  ├─ [3] Frequency: 20-20000Hz, Gain: -12-+12dB, Q: 0.3-10
│  └─ [4] Frequency: 20-20000Hz, Gain: -12-+12dB, Q: 0.3-10
│
├─ range: Pair<Int, Int>
│  └─ Device-specific: typically (-1500, 1500) milliBels
│
├─ supportedBandCount: Int
│  └─ Physical bands on device (may be <10)
│
├─ audioSessionId: Int
│  └─ Current MediaPlayer audio session (or -1 if no playback)
│
└─ error: String?
   └─ Null = OK, non-null = error message
```

---

## 📈 Data Persistence Flow

```
USER ACTION (Settings change)
         ↓
┌────────────────────────┐
│ PlaybackViewModel      │
│ setState() / update()  │
└────────┬───────────────┘
         ↓
┌────────────────────────┐
│ persistEqualizerState()│
│ or updateAudioSettings()
└────────┬───────────────┘
         ↓
┌────────────────────────┐
│ EqualizerStore.save()  │
│ or AudioSettingsStore  │
│      .save()           │
└────────┬───────────────┘
         ↓
┌────────────────────────┐
│ JSON Encode            │
│ (List/Map → String)    │
└────────┬───────────────┘
         ↓
┌────────────────────────┐
│ SharedPreferences      │
│ .putString()           │
└────────┬───────────────┘
         ↓
[DISK - Persistent Storage]
         ↓
[APP RESTART]
         ↓
┌────────────────────────┐
│ EqualizerStore.load()  │
│ or AudioSettingsStore  │
│      .load()           │
└────────┬───────────────┘
         ↓
┌────────────────────────┐
│ SharedPreferences      │
│ .getString()           │
└────────┬───────────────┘
         ↓
┌────────────────────────┐
│ JSON Decode            │
│ (String → List/Map)    │
└────────┬───────────────┘
         ↓
┌────────────────────────┐
│ Emit to StateFlow      │
│ (EqualizerState, etc)  │
└────────┬───────────────┘
         ↓
USER SEES RESTORED STATE
```

---

## 🎨 UI Component Hierarchy

```
MusicAppContent (Main Composable)
│
├─ NavigationBar
│  ├─ Home
│  ├─ Search
│  ├─ Library
│  ├─ Now Playing
│  └─ Settings
│
└─ Screen Content (Conditional)
   │
   ├─ HomeScreen
   │
   ├─ SearchScreen
   │
   ├─ LibraryScreen
   │
   ├─ NowPlayingScreen
   │  └─ Top AppBar
   │     ├─ Equalizer Button ← Opens EqualizerScreen modal
   │     └─ Info Button
   │
   ├─ SettingsScreen
   │  ├─ Audio Quality Card
   │  │  ├─ High-res Toggle
   │  │  ├─ Bit-perfect Toggle
   │  │  └─ Sample Rate Chips
   │  │
   │  └─ Audio Effects Card
   │     ├─ Open Equalizer Button ← Opens EqualizerScreen modal
   │     ├─ Reverb Preset Chips
   │     ├─ Crossfade Slider
   │     └─ Volume Normalization Toggle
   │
   ├─ EqualizerScreen (Modal)
   │  ├─ Graphic Tab
   │  │  ├─ Enable/Disable Toggle
   │  │  ├─ Preset Chips (6 presets)
   │  │  ├─ Frequency Response Chart
   │  │  ├─ Preamp Slider
   │  │  └─ Band Sliders (10 rows)
   │  │
   │  └─ Parametric Tab
   │     ├─ Enable/Disable Toggle
   │     └─ Band Cards (5 bands)
   │        └─ Frequency/Gain/Q Sliders × 5
   │
   └─ ResumePrompt (Banner, conditional)
      ├─ Title + Song Name
      ├─ Resume Button
      └─ Dismiss Button
```

---

**Total Architecture: ~50 lines ASCII art, 4 complex diagrams, 100% coverage of system flows**

