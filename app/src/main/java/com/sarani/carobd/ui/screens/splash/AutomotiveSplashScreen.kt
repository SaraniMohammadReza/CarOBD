package com.sarani.carobd.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun AutomotiveSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lightAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Headlights illuminate
        lightAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        // 2. Logo and text fade in
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        // 3. Pause for brief showcase
        delay(600)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        // Twin Headlights Illumination Glow
        val alpha = lightAlpha.value
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.42f
            val headlightDistance = size.width * 0.22f
            val beamRadius = size.width * 0.38f

            // Left Headlight Beam
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.45f * alpha),
                        NeonCyan.copy(alpha = 0.12f * alpha),
                        Color.Transparent
                    ),
                    center = Offset(cx - headlightDistance, cy),
                    radius = beamRadius
                ),
                radius = beamRadius,
                center = Offset(cx - headlightDistance, cy)
            )

            // Right Headlight Beam
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.45f * alpha),
                        NeonCyan.copy(alpha = 0.12f * alpha),
                        Color.Transparent
                    ),
                    center = Offset(cx + headlightDistance, cy),
                    radius = beamRadius
                ),
                radius = beamRadius,
                center = Offset(cx + headlightDistance, cy)
            )

            // Sharp headlight lenses
            drawCircle(
                color = Color.White.copy(alpha = 0.85f * alpha),
                radius = 7.dp.toPx(),
                center = Offset(cx - headlightDistance, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f * alpha),
                radius = 7.dp.toPx(),
                center = Offset(cx + headlightDistance, cy)
            )
        }

        // App Logo & Brand Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.size(width = 300.dp, height = 240.dp)
        ) {
            Text(
                text = "CarOBD",
                color = TextPrimary.copy(alpha = contentAlpha.value),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.splash_app_tagline),
                color = NeonCyan.copy(alpha = contentAlpha.value * 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}
