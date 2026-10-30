package com.sarani.carobd.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.sarani.carobd.core.storage.AppThemeMode

data class AppPalette(val background: Color, val surface: Color, val card: Color, val border: Color, val primary: Color,
    val text: Color, val secondaryText: Color, val muted: Color)
fun paletteFor(mode: AppThemeMode): AppPalette = when (mode) {
    AppThemeMode.AMOLED_BLACK -> AppPalette(Color.Black, Color(0xFF101010), Color(0xFF181818), Color(0xFF343434), Color(0xFF00E5FF), Color(0xFFF0F6FC), Color(0xFFB1BAC4), Color(0xFF8B949E))
    AppThemeMode.NEON_CYBERPUNK -> AppPalette(Color(0xFF090D14), Color(0xFF121824), Color(0xFF1B2333), Color(0xFF2C394F), Color(0xFF00E5FF), Color(0xFFF0F6FC), Color(0xFFB1BAC4), Color(0xFF8B949E))
    AppThemeMode.RACING_RED -> AppPalette(Color(0xFF14090B), Color(0xFF241216), Color(0xFF331B21), Color(0xFF60303C), Color(0xFFFF526D), Color(0xFFFFF0F3), Color(0xFFE5BEC7), Color(0xFFC28D99))
    AppThemeMode.LIGHT_THEME -> AppPalette(Color(0xFFF5F6FA), Color.White, Color(0xFFE9EDF4), Color(0xFFB9C5D5), Color(0xFF006779), Color(0xFF14202C), Color(0xFF34485C), Color(0xFF4E6275))
}
internal object AppAppearance { var mode by mutableStateOf(AppThemeMode.AMOLED_BLACK) }
private val palette get() = paletteFor(AppAppearance.mode)
val DarkBg get() = palette.background
val DarkSurface get() = palette.surface
val DarkCardBg get() = palette.card
val DarkCardBorder get() = palette.border
val NeonCyan get() = palette.primary
val NeonAmber = Color(0xFFFF9100)
val NeonRed = Color(0xFFFF2A4B)
val NeonGreen = Color(0xFF00B86A)
val NeonBlue = Color(0xFF2979FF)
val TextPrimary get() = palette.text
val TextSecondary get() = palette.secondaryText
val TextMuted get() = palette.muted
val GaugeTrack get() = palette.border.copy(alpha = 0.35f)
val GaugeRedline = NeonRed
val GaugeNormal get() = palette.primary
