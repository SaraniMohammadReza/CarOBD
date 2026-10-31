package com.sarani.carobd.ui.screens.trip

import androidx.compose.ui.res.stringResource
import com.sarani.carobd.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.core.calculator.UnitConverter
import com.sarani.carobd.ui.components.DigitalMetricCard
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.NeonRed
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@Composable
fun TripScreen(
    viewModel: TripViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isRecording = state.summary.isRecording

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(com.sarani.carobd.R.string.ui_trip_title),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRecording) stringResource(com.sarani.carobd.R.string.s5_trip_recording) else stringResource(com.sarani.carobd.R.string.s5_trip_ready),
                        color = if (isRecording) NeonGreen else TextMuted,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { viewModel.onIntent(TripIntent.ToggleRecording) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) NeonRed else NeonGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DarkBg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecording) stringResource(com.sarani.carobd.R.string.s5_trip_stop) else stringResource(com.sarani.carobd.R.string.s5_trip_start),
                        color = DarkBg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Trip Stats Grid
            val minutes = state.summary.durationSeconds / 60
            val seconds = state.summary.durationSeconds % 60
            val durationFormatted = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val (distanceValue, distanceUnit) = UnitConverter.formatDistance(
                    state.summary.distanceKm.toDouble(),
                    state.unitSystem
                )
                DigitalMetricCard(
                    title = stringResource(com.sarani.carobd.R.string.s5_trip_distance),
                    value = if (state.summary.evidence.any { it.metric == com.sarani.carobd.core.model.TripMetric.DISTANCE && it.value != null }) distanceValue else "—",
                    unit = distanceUnit,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                DigitalMetricCard(
                    title = stringResource(com.sarani.carobd.R.string.s5_trip_time),
                    value = durationFormatted,
                    unit = stringResource(com.sarani.carobd.R.string.s5_minute),
                    icon = Icons.Default.Timer,
                    accentColor = NeonAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val (maxSpeedValue, maxSpeedUnit) = UnitConverter.formatSpeed(
                    state.summary.maxSpeedKmh.toDouble(),
                    state.unitSystem
                )
                DigitalMetricCard(
                    title = stringResource(com.sarani.carobd.R.string.s5_max_speed),
                    value = if (state.summary.speedSamples > 0) maxSpeedValue else "—",
                    unit = maxSpeedUnit,
                    icon = Icons.Default.Speed,
                    accentColor = NeonRed,
                    modifier = Modifier.weight(1f)
                )

                val (avgSpeedValue, avgSpeedUnit) = UnitConverter.formatSpeed(
                    state.summary.averageSpeedKmh.toDouble(),
                    state.unitSystem
                )
                DigitalMetricCard(
                    title = stringResource(com.sarani.carobd.R.string.s5_avg_speed),
                    value = if (state.summary.evidence.any { it.metric == com.sarani.carobd.core.model.TripMetric.AVERAGE_SPEED && it.value != null }) avgSpeedValue else "—",
                    unit = avgSpeedUnit,
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DigitalMetricCard(
                    title = stringResource(com.sarani.carobd.R.string.s5_max_rpm),
                    value = if (state.summary.rpmSamples > 0) "${state.summary.maxRpm}" else "—",
                    unit = "RPM",
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                DigitalMetricCard(
                    title = stringResource(com.sarani.carobd.R.string.s5_fuel_est),
                    value = state.summary.estimatedFuelUsedLiters?.let { com.sarani.carobd.ui.theme.displayMeasurement(it, "L", com.sarani.carobd.ui.theme.LocalUnitSystem.current) }?.let { String.format(java.util.Locale.US, "%.2f", it.value) } ?: "—",
                    unit = if (state.unitSystem == com.sarani.carobd.core.storage.UnitSystem.IMPERIAL) "US gal" else "L",
                    accentColor = NeonAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (state.summary.coverageLimited || state.summary.limitReached || state.summary.estimatedFuelUsedLiters == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (state.summary.coverageLimited || state.summary.limitReached) {
                            Text(stringResource(R.string.trip_partial), color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        if (state.summary.estimatedFuelUsedLiters == null) {
                            Text(stringResource(R.string.trip_fuel_unknown), color = TextMuted, fontSize = 11.sp)
                        }
                        Text(stringResource(R.string.s5_trip_limits), color = TextMuted, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            com.sarani.carobd.ui.components.TripEvidenceSummary(state.summary.evidence)
            Spacer(modifier = Modifier.height(14.dp))

            // Driving Quality & Events Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(com.sarani.carobd.R.string.ui_driving_events), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(com.sarani.carobd.R.string.ui_hard_accel), color = TextMuted, fontSize = 12.sp)
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.s5_times, state.summary.hardAccelerationsCount, state.summary.hardAccelerationsCount), color = if (state.summary.hardAccelerationsCount > 3) NeonRed else NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(com.sarani.carobd.R.string.ui_hard_brake), color = TextMuted, fontSize = 12.sp)
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.s5_times, state.summary.hardBrakingCount, state.summary.hardBrakingCount), color = if (state.summary.hardBrakingCount > 2) NeonRed else NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(com.sarani.carobd.R.string.ui_sample_count), color = TextMuted, fontSize = 12.sp)
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.s5_frames, state.summary.samplesCount, state.summary.samplesCount), color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Export to CSV Button
            OutlinedButton(
                onClick = { viewModel.onIntent(TripIntent.ExportCsv) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(com.sarani.carobd.R.string.ui_trip_export), fontWeight = FontWeight.Bold)
            }

            state.exportedCsvSnippet?.let { snippet ->
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, RoundedCornerShape(10.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(stringResource(com.sarani.carobd.R.string.ui_csv_preview), color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(snippet, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
