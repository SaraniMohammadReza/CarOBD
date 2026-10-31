package com.sarani.carobd.ui.design

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Canonical Spacing Tokens for CarOBD.
 * Prevents arbitrary per-screen spacing drift.
 */
object Spacing {
    val micro = 4.dp
    val compact = 8.dp
    val medium = 12.dp
    val standard = 16.dp
    val wide = 20.dp
    val section = 24.dp
    val major = 32.dp
}

/**
 * Canonical Corner Radii Tokens for CarOBD cards, chips, buttons and dialogs.
 */
object CornerRadius {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xlarge = 24.dp

    val shapeSmall = RoundedCornerShape(small)
    val shapeMedium = RoundedCornerShape(medium)
    val shapeLarge = RoundedCornerShape(large)
    val shapeXLarge = RoundedCornerShape(xlarge)
    val shapePill = CircleShape
}

/**
 * Strict Semantic Status Colors for CarOBD:
 * - CRITICAL/RED: Errors, failed tests, overdue critical service only.
 * - WARNING/AMBER: Service due soon, pending issues, unverified safety items.
 * - SUCCESS/GREEN: Confirmed valid/completed items only (NEVER fake "vehicle OK").
 * - INFO/PRIMARY: Active navigation, informative banners.
 * - NEUTRAL/GRAY: Unknown, unconfigured, or inactive states.
 */
object SemanticColors {
    val success = Color(0xFF2E7D32)
    val successContainer = Color(0xFFE8F5E9)
    val onSuccessContainer = Color(0xFF1B5E20)

    val warning = Color(0xFFEF6C00)
    val warningContainer = Color(0xFFFFF3E0)
    val onWarningContainer = Color(0xFFE65100)

    val critical = Color(0xFFC62828)
    val criticalContainer = Color(0xFFFFEBEE)
    val onCriticalContainer = Color(0xFFB71C1C)

    val info = Color(0xFF1565C0)
    val infoContainer = Color(0xFFE3F2FD)
    val onInfoContainer = Color(0xFF0D47A1)

    val neutralMuted = Color(0xFF757575)
    val neutralMutedContainer = Color(0xFFEEEEEE)
    val onNeutralMutedContainer = Color(0xFF424242)
}

/**
 * Canonical Elevation Tokens for CarOBD surfaces.
 */
object Elevation {
    val none = 0.dp
    val card = 1.dp
    val elevated = 2.dp
    val dialog = 6.dp
}

/**
 * Canonical Component Sizing Tokens for touch targets, icons, and layout constraints.
 */
object CarObdSizes {
    val minTouchTarget = 48.dp
    val iconMicro = 14.dp
    val iconSmall = 18.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val iconHero = 48.dp
    val buttonHeight = 48.dp
}
