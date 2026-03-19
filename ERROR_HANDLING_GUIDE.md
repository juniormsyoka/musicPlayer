# Error Handling Implementation Guide

## Overview
This guide provides the implementation roadmap for adding comprehensive error handling and user feedback to the Music App screens.

---

## 1. Error Handling Architecture

### Current State
- `LibraryState` has optional `error: String?` field
- `PlayerState.Error` exists for playback errors
- No UI feedback for errors currently

### Proposed Additions

#### A. SnackBar Notifications
Create a reusable error notification system:

```kotlin
// In MusicAppContent.kt
@Composable
fun MusicAppContent(
    // ... existing parameters ...
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // ... rest of scaffold ...
    ) {
        // Watch for errors and show snackbar
        LaunchedEffect(libraryState.error) {
            libraryState.error?.let {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
}
```

#### B. Error Recovery Dialogs
Add retry mechanisms:

```kotlin
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onRetry) { Text("Retry") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

---

## 2. Screen-Specific Error Handling

### HomeScreen
**Errors to Handle:**
- Library load failure
- Permission denied
- Storage access errors

**Implementation:**
```kotlin
@Composable
fun HomeScreen(
    libraryState: LibraryState,
    // ... existing parameters ...
) {
    // Show error banner at top
    if (libraryState.error != null) {
        ErrorBanner(
            message = libraryState.error!!,
            onDismiss = { /* Clear error */ }
        )
    }
    
    // Rest of screen...
}
```

### SearchScreen
**Errors to Handle:**
- Search timeout
- Invalid search syntax
- No results (not an error, but handle gracefully)

**Implementation:**
```kotlin
if (searchQuery.isNotEmpty() && filteredSongs.isEmpty()) {
    EmptySearchResults(
        query = searchQuery,
        suggestionText = "Try different keywords or check filters"
    )
}
```

### LibraryScreen
**Errors to Handle:**
- Playlist creation failure
- Invalid playlist name
- Duplicate playlist names
- Folder read permission errors

**Implementation:**
```kotlin
if (showCreatePlaylistDialog) {
    CreatePlaylistDialog(
        onDismiss = { showCreatePlaylistDialog = false },
        onConfirm = { name ->
            if (isValidPlaylistName(name)) {
                // createPlaylist(name)
            } else {
                // Show error: "Invalid playlist name"
            }
        }
    )
}
```

### NowPlayingScreen
**Errors to Handle:**
- Playback failed
- File not found
- Audio format unsupported
- Codec missing

**Implementation:**
Already implemented with PlayerState.Error display. Enhance with:

```kotlin
is PlayerState.Error -> {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Playback Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        Button(onClick = { /* Retry */ }) {
            Text("Try Again")
        }
    }
}
```

---

## 3. User Feedback Patterns

### Loading States
- Show `CircularProgressIndicator` during operations
- Disable buttons during async operations
- Display percentage for large operations

### Success Feedback
```kotlin
snackbarHostState.showSnackbar(
    message = "Playlist created successfully",
    duration = SnackbarDuration.Short
)
```

### Error Feedback
```kotlin
snackbarHostState.showSnackbar(
    message = "Failed to create playlist: Duplicate name",
    duration = SnackbarDuration.Long
)
```

### Empty State
```kotlin
Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.AudioFile,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("No songs found")
        Text("Try checking your music folder", style = MaterialTheme.typography.bodySmall)
    }
}
```

---

## 4. Implementation Checklist

### Phase 1: Basic Error UI
- [ ] Add SnackbarHostState to MusicAppContent
- [ ] Create ErrorBanner composable
- [ ] Enhance PlaybackError display
- [ ] Add error color to theme

### Phase 2: Screen Integration
- [ ] HomeScreen - library load errors
- [ ] SearchScreen - validation errors
- [ ] LibraryScreen - playlist operation errors
- [ ] NowPlayingScreen - enhance error display

### Phase 3: User Feedback
- [ ] Add loading indicators for async operations
- [ ] Show success messages for operations
- [ ] Add helpful error suggestions
- [ ] Implement retry mechanisms

### Phase 4: Testing
- [ ] Test all error paths
- [ ] Verify user can recover from errors
- [ ] Check error message clarity
- [ ] Performance test with large error queues

---

## 5. Error Message Guidelines

### Be Specific
❌ Bad: "Error occurred"
✅ Good: "Failed to create playlist: A playlist with this name already exists"

### Be Actionable
❌ Bad: "Unknown error"
✅ Good: "Music file not found. Check that the file still exists on your device."

### Be Brief
❌ Bad: "An unexpected error occurred while attempting to process your request. Please try again or contact support."
✅ Good: "Failed to load music. Please try again."

---

## 6. Code Examples

### Creating a Reusable Error Handler
```kotlin
sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
    data class ShowSuccess(val message: String) : UiEvent()
}

@Composable
fun ErrorHandler(
    events: Flow<UiEvent>,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is UiEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}
```

### Permission Error Handling
```kotlin
fun hasAudioPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        requiredPermission()
    ) == PackageManager.PERMISSION_GRANTED
}

// In MainActivity.kt
if (!hasAudioPermission(context)) {
    audioPermissionLauncher.launch(requiredPermission())
    // Show snackbar: "Music permission required to load your library"
}
```

---

## 7. Testing Error Scenarios

### Simulating Errors
```kotlin
// In ViewModels - add debug flag
private val DEBUG_MODE = BuildConfig.DEBUG

fun refreshLibrary(simulateError: Boolean = false) {
    if (simulateError) {
        _state.update { it.copy(error = "Simulated error for testing") }
        return
    }
    // Normal flow
}
```

### Manual Testing Checklist
- [ ] Disconnect device from storage
- [ ] Revoke app permissions
- [ ] Load playlist with deleted songs
- [ ] Delete song while playing
- [ ] Fill device storage to capacity
- [ ] Create playlist with special characters in name

---

## 8. Resources & References

- Material Design Error States: https://m3.material.io/
- Jetpack Compose Snackbar: https://developer.android.com/jetpack/compose/snackbars
- Android Error Handling: https://developer.android.com/guide/topics/data/backup/backupagent

---

**Status:** Ready for implementation
**Estimated Time:** 2-3 hours
**Priority:** MEDIUM (after core screens completed)

