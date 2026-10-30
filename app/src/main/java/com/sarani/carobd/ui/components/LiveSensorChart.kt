package com.sarani.carobd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@Composable
fun LiveSensorChart(
    title: String,
    history: List<Float>,
    unit: String,
    accentColor: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    val currentVal = history.lastOrNull() ?: 0f
    val maxVal = (history.maxOrNull() ?: 100f).coerceAtLeast(10f)
    val minVal = (history.minOrNull() ?: 0f).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(14.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title and Current Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sarani.carobd.R.string.s5_chart_bounds, maxVal.toInt(), minVal.toInt(), unit),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", currentVal),
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " $unit",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(DarkBg.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                val w = size.width
                val h = size.height

                // Draw 3 horizontal grid lines
                for (i in 1..3) {
                    val y = h * (i / 4f)
                    drawLine(
                        color = DarkCardBorder.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                if (history.size < 2) return@Canvas

                val range = if (maxVal > minVal) (maxVal - minVal) else 1f
                val stepX = w / (history.size - 1)

                val linePath = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, h)

                history.forEachIndexed { index, value ->
                    val x = index * stepX
                    val normalizedY = ((value - minVal) / range).coerceIn(0f, 1f)
                    val y = h - (normalizedY * h)

                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo((history.size - 1) * stepX, h)
                fillPath.close()

                // Draw gradient under-fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.05f)
                        )
                    )
                )

                // Draw stroke line
                drawPath(
                    path = linePath,
                    color = accentColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw dot at latest point
                val lastX = (history.size - 1) * stepX
                val lastNormalizedY = ((currentVal - minVal) / range).coerceIn(0f, 1f)
                val lastY = h - (lastNormalizedY * h)

                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = accentColor,
                    radius = 6.dp.toPx(),
                    center = Offset(lastX, lastY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}
