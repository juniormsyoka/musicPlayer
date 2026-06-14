package com.vybzvault.music

import android.content.Context
import androidx.core.content.edit

class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("music_store", Context.MODE_PRIVATE)

    fun getFavorites(): Set<Long> {
        return prefs.getStringSet(PrefKeys.FAVORITES, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    fun toggleFavorite(songId: Long): Set<Long> {
        val next = getFavorites().toMutableSet()
        if (!next.add(songId)) {
            next.remove(songId)
        }
        prefs.edit {
            putStringSet(PrefKeys.FAVORITES, next.map { it.toString() }.toSet())
        }
        return next
    }
}

