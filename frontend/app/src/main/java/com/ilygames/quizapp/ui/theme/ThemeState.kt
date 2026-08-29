package com.ilygames.quizapp.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeState {
    var isDarkMode by mutableStateOf(false)
    var isSoundEnabled by mutableStateOf(true)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean("is_dark_mode", false)
        isSoundEnabled = prefs.getBoolean("is_sound_enabled", true)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        isDarkMode = enabled
        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        isSoundEnabled = enabled
        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_sound_enabled", enabled).apply()
    }
}
