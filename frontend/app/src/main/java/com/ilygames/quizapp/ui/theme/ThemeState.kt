package com.ilygames.quizapp.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeState {
    var isDarkMode by mutableStateOf(false)
    var isSoundEnabled by mutableStateOf(true)
}
