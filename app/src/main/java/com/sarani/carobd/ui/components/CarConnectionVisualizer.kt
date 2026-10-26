package com.sarani.carobd.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@Composable
fun CarConnectionVisualizer(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = !(connectionState is ConnectionState.Connected ||
        connectionState is ConnectionState.Disconnected ||
        connectionState is ConnectionState.Disconnecting ||
        connectionState is ConnectionState.Failure ||
        connectionState is ConnectionState.Idle ||
        connectionState is ConnectionState.PermissionRequired)

    val infiniteTransition = rememberInfiniteTransition(label = "CarAnimation")

    // Pulse animation for energy stream
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    // Headlight glow breathe effect
    val headlightGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeadlightGlow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                val w = size.width
                val h = size.height

                val carY = h * 0.72f
                val carStartX = w * 0.12f
                val carEndX = w * 0.88f
                val carWidth = carEndX - carStartX

                // 1. Road Line
                drawLine(
                    color = DarkCardBorder.copy(alpha = 0.7f),
                    start = Offset(0f, carY + 12f),
                    end = Offset(w, carY + 12f),
                    strokeWidth = 2.dp.toPx()
                )

                // 2. Neon Underglow when Connected
                if (isConnected) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.45f * headlightGlow), Color.Transparent),
                            center = Offset(w * 0.5f, carY + 6f),
                            radius = carWidth * 0.45f
                        ),
                        topLeft = Offset(carStartX, carY - 10f),
                        size = Size(carWidth, 26.dp.toPx())
                    )
                }

                // 3. Car Silhouette Path (Sleek aerodynamic coupe / sedan)
                val carPath = Path().apply {
                    // Rear Bumper
                    moveTo(carStartX, carY)
                    // Rear Wheel Arch
                    val rearWheelCenterX = carStartX + (carWidth * 0.18f)
                    val wheelRadius = 14.dp.toPx()
                    lineTo(rearWheelCenterX - wheelRadius, carY)
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            center = Offset(rearWheelCenterX, carY),
                            radius = wheelRadius
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    // Underbody
                    val frontWheelCenterX = carStartX + (carWidth * 0.78f)
                    lineTo(frontWheelCenterX - wheelRadius, carY)
                    // Front Wheel Arch
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            center = Offset(frontWheelCenterX, carY),
                            radius = wheelRadius
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    // Front Bumper / Splitter
                    lineTo(carEndX, carY)
                    lineTo(carEndX + 4f, carY - 6f)
                    // Hood / Bonnet
                    lineTo(carStartX + (carWidth * 0.75f), carY - 18f)
                    // Windshield
                    lineTo(carStartX + (carWidth * 0.52f), carY - 38f)
                    // Roofline
                    lineTo(carStartX + (carWidth * 0.32f), carY - 38f)
                    // Rear Window / Fastback
                    lineTo(carStartX + (carWidth * 0.14f), carY - 18f)
                    // Trunk / Boot lid
                    lineTo(carStartX, carY - 12f)
                    close()
                }

                // Draw Car Body Fill
                drawPath(
                    path = carPath,
                    color = if (isConnected) DarkBg else DarkBg.copy(alpha = 0.8f),
                    style = Fill
                )

                // Draw Car Body Outline
                val outlineColor = when {
                    isConnected -> NeonCyan
                    isConnecting -> NeonAmber
                    else -> DarkCardBorder
                }
                drawPath(
                    path = carPath,
                    color = outlineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // 4. Wheels
                val wheelRadius = 11.dp.toPx()
                val rearWheelX = carStartX + (carWidth * 0.18f)
                val frontWheelX = carStartX + (carWidth * 0.78f)

                // Rear Wheel
                drawCircle(color = DarkCardBorder, radius = wheelRadius, center = Offset(rearWheelX, carY))
                drawCircle(color = if (isConnected) NeonCyan else TextMuted, radius = 4.dp.toPx(), center = Offset(rearWheelX, carY))

                // Front Wheel
                drawCircle(color = DarkCardBorder, radius = wheelRadius, center = Offset(frontWheelX, carY))
                drawCircle(color = if (isConnected) NeonCyan else TextMuted, radius = 4.dp.toPx(), center = Offset(frontWheelX, carY))

                // 5. OBD-II Connection Energy Beam (Flows from OBD port to Engine ECU to Headlights)
                val obdPortX = carStartX + (carWidth * 0.42f)
                val obdPortY = carY - 18f
                val headlightX = carEndX + 2f
                val headlightY = carY - 12f

                if (isConnecting) {
                    val currentBeamX = obdPortX + ((headlightX - obdPortX) * pulseProgress)
                    drawLine(
                        color = NeonAmber.copy(alpha = 0.3f),
                        start = Offset(obdPortX, obdPortY),
                        end = Offset(headlightX, headlightY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Moving pulse ball
                    drawCircle(
                        color = NeonAmber,
                        radius = 5.dp.toPx(),
                        center = Offset(currentBeamX, obdPortY + ((headlightY - obdPortY) * pulseProgress))
                    )
                }

                // 6. Headlights (Bright Neon Beam when CONNECTED!)
                if (isConnected) {
                    // Headlight Lens
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = Offset(headlightX, headlightY)
                    )

                    // Forward Light Beam Cone
                    val lightBeamPath = Path().apply {
                        moveTo(headlightX, headlightY - 3f)
                        lineTo(w, headlightY - 18f)
                        lineTo(w, carY + 8f)
                        lineTo(headlightX, headlightY + 5f)
                        close()
                    }

                    drawPath(
                        path = lightBeamPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.65f * headlightGlow),
                                NeonCyan.copy(alpha = 0.25f * headlightGlow),
                                Color.Transparent
                            ),
                            startX = headlightX,
                            endX = w
                        )
                    )

                    // Taillight Red Glow
                    drawCircle(
                        color = Color(0xFFFF2A4B),
                        radius = 3.5.dp.toPx(),
                        center = Offset(carStartX + 2f, carY - 12f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle text indicating visual state
            val (descText, descColor) = when (val cs = connectionState) {
                is ConnectionState.Connected -> "چراغ‌های خودرو روشن شد • ایسیو آنلاین و متصل است" to NeonGreen
                is ConnectionState.Failure -> "عدم پاسخگویی ایسیو یا قطعی سوییچ خودرو" to Color(0xFFFF2A4B)
                is ConnectionState.Disconnected, is ConnectionState.Idle -> "آماده اتصال • دانگل دیاگ را متصل نمایید" to TextMuted
                is ConnectionState.Disconnecting -> "در حال قطع ارتباص..." to TextMuted
                is ConnectionState.AdapterIdentifying -> "هماهنم‌سازی بایت‌های فریم و ارتباط با ایسیو (${cs.probeStage})..." to NeonAmber
                is ConnectionState.AdapterConfiguring -> "تنظیمات آداپتور (${cs.setting})..." to NeonAmber
                is ConnectionState.ProtocolDetecting -> "جستجوی پروتکل ${cs.candidateName} (${cs.attemptIndex}/${cs.totalCandidates})..." to NeonAmber
                else -> "در حال ارسال سیگنال و یافتن پورت OBD-II..." to NeonAmber
            }

            Text(
                text = descText,
                color = descColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
