package com.sarani.carobd.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonRed
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularGauge(
    value: Float,
    minValue: Float = 0f,
    maxValue: Float = 8000f,
    title: String,
    unit: String,
    redlineThreshold: Float = maxValue * 0.8f,
    modifier: Modifier = Modifier,
    primaryColor: Color = NeonCyan,
    warningColor: Color = NeonRed
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(minValue, maxValue),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 180f),
        label = "GaugeNeedleAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 16.dp.toPx()
            val strokeWidth = 14.dp.toPx()

            val startAngle = 135f
            val sweepAngle = 270f

            // 1. Background Track Arc
            drawArc(
                color = DarkCardBorder,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 2. Redline Zone Background Arc
            val redlineRatio = ((redlineThreshold - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
            val redlineStartAngle = startAngle + (sweepAngle * redlineRatio)
            val redlineSweep = sweepAngle * (1f - redlineRatio)

            drawArc(
                color = warningColor.copy(alpha = 0.35f),
                startAngle = redlineStartAngle,
                sweepAngle = redlineSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 3. Active Value Progress Arc
            val progressRatio = ((animatedValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
            val currentSweep = sweepAngle * progressRatio

            val currentColor = if (animatedValue >= redlineThreshold) warningColor else primaryColor

            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to primaryColor.copy(alpha = 0.6f),
                    redlineRatio to NeonAmber,
                    1.0f to warningColor,
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = currentSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 4. Tick Marks
            val tickCount = 10
            for (i in 0..tickCount) {
                val tickRatio = i.toFloat() / tickCount
                val angleDeg = startAngle + (sweepAngle * tickRatio)
                val angleRad = (angleDeg * PI / 180f).toFloat()

                val innerRadius = radius - 18.dp.toPx()
                val outerRadius = radius - 8.dp.toPx()

                val start = Offset(
                    x = center.x + innerRadius * cos(angleRad),
                    y = center.y + innerRadius * sin(angleRad)
                )
                val end = Offset(
                    x = center.x + outerRadius * cos(angleRad),
                    y = center.y + outerRadius * sin(angleRad)
                )

                val tickColor = if (tickRatio >= redlineRatio) warningColor else DarkCardBorder
                drawLine(
                    color = tickColor,
                    start = start,
                    end = end,
                    strokeWidth = if (i % 2 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 5. Dial Needle
            val needleAngleDeg = startAngle + (sweepAngle * progressRatio)
            val needleAngleRad = (needleAngleDeg * PI / 180f).toFloat()
            val needleLength = radius - 24.dp.toPx()

            val needleEnd = Offset(
                x = center.x + needleLength * cos(needleAngleRad),
                y = center.y + needleLength * sin(needleAngleRad)
            )

            // Needle Line
            drawLine(
                color = currentColor,
                start = center,
                end = needleEnd,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center Pivot Circle
            drawCircle(
                color = currentColor,
                radius = 7.dp.toPx(),
                center = center
            )
            drawCircle(
                color = TextPrimary,
                radius = 3.dp.toPx(),
                center = center
            )
        }

        // Digital Readout in Center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 70.dp)
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${animatedValue.toInt()}",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = unit,
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
