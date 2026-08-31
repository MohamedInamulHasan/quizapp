package com.ilygames.quizapp.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeState {
    // App is PERMANENTLY DARK MODE ONLY
    var isDarkMode by mutableStateOf(true)
    var isSoundEnabled by mutableStateOf(true)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        isDarkMode = true
        isSoundEnabled = prefs.getBoolean("is_sound_enabled", true)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        isDarkMode = true
        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_dark_mode", true).apply()
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        isSoundEnabled = enabled
        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_sound_enabled", enabled).apply()
    }
}
