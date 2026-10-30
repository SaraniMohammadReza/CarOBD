package com.sarani.carobd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sarani.carobd.R
import com.sarani.carobd.core.model.TelemetryGraphHistory
import com.sarani.carobd.core.model.TelemetryGraphPoint

/** Timestamp axis and separate paths: no line or fill can cross missing/stale data. */
@Composable
fun LiveSensorChart(title: String, history: List<TelemetryGraphPoint>, currentValue: Float?, quality: com.sarani.carobd.core.model.SignalQuality, unit: String,
    accentColor: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    val bounded = history.takeLast(TelemetryGraphHistory.MAX_POINTS)
    val segments = TelemetryGraphHistory.segments(bounded)
    val values = segments.flatten().mapNotNull { it.value }
    val minimum = values.minOrNull() ?: 0f
    val maximum = values.maxOrNull() ?: 1f
    val range = (maximum - minimum).takeIf { it > 0f } ?: 1f
    val firstTime = bounded.firstOrNull()?.timeMs ?: 0L
    val duration = ((bounded.lastOrNull()?.timeMs ?: firstTime) - firstTime).coerceAtLeast(1L)
    val grid = MaterialTheme.colorScheme.outlineVariant
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text((currentValue?.takeIf { it.isFinite() }?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—") + " $unit",
                style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(when (quality) {
                com.sarani.carobd.core.model.SignalQuality.GOOD -> R.string.graph_current
                com.sarani.carobd.core.model.SignalQuality.STALE -> R.string.signal_stale
                else -> R.string.graph_unavailable
            }), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.graph_gap_help), style = MaterialTheme.typography.bodySmall)
            Canvas(Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceContainer)) {
                repeat(3) { index ->
                    val y = size.height * (index + 1) / 4f
                    drawLine(grid, Offset(0f, y), Offset(size.width, y))
                }
                fun offset(point: TelemetryGraphPoint): Offset = Offset(
                    ((point.timeMs - firstTime).toDouble() / duration * size.width).toFloat(),
                    size.height * (1f - ((point.value!! - minimum) / range).coerceIn(0f, 1f)))
                for (segment in segments) {
                    val path = Path()
                    segment.forEachIndexed { index, point ->
                        val position = offset(point)
                        if (index == 0) path.moveTo(position.x, position.y) else path.lineTo(position.x, position.y)
                    }
                    drawPath(path, accentColor, style = Stroke(width = 2.dp.toPx()))
                    if (segment.size == 1) drawCircle(accentColor, 2.dp.toPx(), offset(segment.single()))
                }
            }
        }
    }
}
