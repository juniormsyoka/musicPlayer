package com.vybzvault.music

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vybzvault.music.home.HomeStateCalculator
import com.vybzvault.music.time.SystemTimeProvider

class PlaybackViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaybackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaybackViewModel(
                context = application.applicationContext,
                libraryStore = LibraryStore(application.applicationContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class LibraryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(
                repository = MusicRepository(application.applicationContext),
                store = LibraryStore(application.applicationContext),
                timeProvider = SystemTimeProvider(),
                homeStateCalculator = HomeStateCalculator()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}