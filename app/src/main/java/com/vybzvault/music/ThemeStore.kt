package com.vybzvault.music

import android.content.Context
import androidx.core.content.edit
import com.vybzvault.music.ui.theme.AppThemePreset

class ThemeStore(context: Context) {

    private val prefs = context.getSharedPreferences("theme_store", Context.MODE_PRIVATE)

    fun load(): AppThemePreset {
        val themeName = prefs.getString(KEY_SELECTED_THEME, AppThemePreset.DEFAULT.name)
        return AppThemePreset.entries.firstOrNull {
            it.name == themeName
        } ?: AppThemePreset.DEFAULT
    }

    fun save(theme: AppThemePreset) {
        prefs.edit {
            putString(KEY_SELECTED_THEME, theme.name)
        }
    }

    companion object {
        private const val KEY_SELECTED_THEME = "selected_theme"
    }
}
