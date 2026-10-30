package com.sarani.carobd.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sarani.carobd.core.storage.AppPreferencesManager
import com.sarani.carobd.core.storage.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkBg,
    secondary = NeonAmber,
    onSecondary = DarkBg,
    tertiary = NeonGreen,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

/** Daytime High Contrast light scheme (§Phase C): neon accents on a light surface. */
private val LightColorScheme = lightColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF0A0A0A),
    secondary = NeonAmber,
    onSecondary = Color(0xFF0A0A0A),
    tertiary = NeonGreen,
    background = Color(0xFFF5F6FA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1B1B1B),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

@Composable
fun CarObdTheme(
    appPreferencesManager: AppPreferencesManager,
    content: @Composable () -> Unit
) {
    // Theme choice is persisted (AppThemeMode) and observed app-wide (§Phase C).
    val themeMode by appPreferencesManager.themeMode.collectAsState(AppThemeMode.AMOLED_BLACK)

    val units by appPreferencesManager.unitSystem.collectAsState(com.sarani.carobd.core.storage.UnitSystem.METRIC)
    val palette = paletteFor(themeMode)
    SideEffect { AppAppearance.mode = themeMode }
    val isDarkTheme = themeMode != AppThemeMode.LIGHT_THEME
    val colorScheme = (if (isDarkTheme) DarkColorScheme else LightColorScheme).copy(
        primary = palette.primary,
        onPrimary = if (isDarkTheme) palette.background else Color.White,
        primaryContainer = palette.card, onPrimaryContainer = palette.primary,
        background = palette.background, surface = palette.surface,
        surfaceContainer = palette.card, surfaceContainerHigh = palette.card,
        surfaceContainerHighest = palette.card, surfaceContainerLow = palette.surface,
        surfaceContainerLowest = palette.background,
        surfaceVariant = palette.card, onBackground = palette.text, onSurface = palette.text,
        onSurfaceVariant = palette.secondaryText, outline = palette.border)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor =
                    if (isDarkTheme) DarkBg.toArgb() else LightColorScheme.background.toArgb()
                window.navigationBarColor =
                    if (isDarkTheme) DarkBg.toArgb() else LightColorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalUnitSystem provides units) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
