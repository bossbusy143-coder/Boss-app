package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    private const val PREFS_NAME = "gyansarthi_theme_prefs"
    private const val KEY_IS_DARK = "key_is_dark_theme"

    var isDarkTheme by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean(KEY_IS_DARK, true)
    }

    fun toggleTheme(context: Context) {
        setTheme(context, !isDarkTheme)
    }

    fun setTheme(context: Context, dark: Boolean) {
        isDarkTheme = dark
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_DARK, dark).apply()
    }
}
