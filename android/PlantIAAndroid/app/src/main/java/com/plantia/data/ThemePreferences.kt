package com.plantia.data

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var mode: ThemeMode
        get() = ThemeMode.entries.getOrElse(prefs.getInt(KEY_MODE, 0)) { ThemeMode.SYSTEM }
        set(value) {
            prefs.edit().putInt(KEY_MODE, value.ordinal).apply()
        }

    @Composable
    fun isDarkTheme(): Boolean = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    companion object {
        private const val PREFS_NAME = "plantia_theme"
        private const val KEY_MODE = "mode"
    }
}
