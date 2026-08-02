package com.ilygames.quizapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkGlassColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    secondary = ElectricMint,
    tertiary = EmeraldGlow,
    background = DarkSlateBg,
    surface = GlassSurface,
    onPrimary = TextWhite,
    onSecondary = DarkSlateBg,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = GlassBorder
)

private val LightGreenColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    secondary = DarkGreen,
    tertiary = ElectricMint,
    background = LightBackground,
    surface = CardWhite,
    onPrimary = CardWhite,
    onSecondary = CardWhite,
    onTertiary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = SurfaceGray
)

@Composable
fun QuizAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = if (ThemeState.isDarkMode) DarkGlassColorScheme else LightGreenColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !ThemeState.isDarkMode
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
