package com.sarani.carobd.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.core.database.SensorCatalog
import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.model.DashboardWidgetConfig
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.WidgetSize
import com.sarani.carobd.core.model.WidgetType
import com.sarani.carobd.ui.components.BluetoothDeviceDialog
import com.sarani.carobd.ui.components.CircularGauge
import com.sarani.carobd.ui.components.DigitalMetricCard
import com.sarani.carobd.ui.components.HorizontalBarGauge
import com.sarani.carobd.ui.components.LiveSensorChart
import com.sarani.carobd.ui.components.SensorPickerModal
import com.sarani.carobd.ui.components.StatusHeader
import com.sarani.carobd.ui.components.VehicleOnboardingModal
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
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    onNavigateToVehicleCatalog: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val currentTime by telemetryClock()
    val recording by viewModel.recordingState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .graphicsLayer {
                if (state.isHudMode) {
                    scaleX = -1f
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Status Header
            item { com.sarani.carobd.ui.components.RuntimeAccessControls(); Text(recording); Text(state.activeConnectionName ?: state.connectingName ?: "—") }
            item {
                StatusHeader(
                    connectionState = state.connectionState,
                    activeProfile = state.activeProfile,
                    batteryVoltage = state.telemetry.signal("ATRV", currentTime).takeIf { it.quality == com.sarani.carobd.core.model.SignalQuality.GOOD }?.value,
                    onConnectClicked = { viewModel.onIntent(DashboardIntent.ToggleConnection) },
                    onDisconnectClicked = { viewModel.onIntent(DashboardIntent.ToggleConnection) }
                )
            }

            // 1.1 Diagnostic vehicle selection CTA (§2/§C4: free-text onboarding leaves activeVehicle null;
            //     must NOT fuzz-match make/model — navigate to the exact catalog Vehicle screen.)
            if (!state.hasActiveVehicle) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonAmber.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.dashboard_no_vehicle), color = TextPrimary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { onNavigateToVehicleCatalog() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonAmber.copy(alpha = 0.2f),
                                    contentColor = NeonAmber
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(stringResource(R.string.dashboard_select_profile), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 1.2 Primary metrics row (§4): Speed / RPM / Coolant / Battery. Unknown → "—";
            //     never fabricate a zero value (§4/§7).
            item {
                PrimaryMetricsRow(telemetry = state.telemetry)
            }

            // 2. Simulator Warning Banner — §4: keep a visible SIMULATOR indicator.
            //    The animated CarConnectionVisualizer (infinite Canvas animation,
            //    heavy on the J7) has been removed from the first screen; connection
            //    state is already shown in StatusHeader above.
            if (state.isSimulator) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonAmber.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.simulator_warning),
                                color = NeonAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Fuel Economy & Driving Range Prediction Banner
            item {
                FuelEconomyBanner(
                    instantL100km = state.fuelEconomy.instantLitersPer100Km,
                    remainingRangeKm = state.fuelEconomy.remainingRangeKm,
                    costPerKm = state.fuelEconomy.fuelCostPerKmTomans,
                    vehicleName = state.vehicleDisplayLabel()
                )
            }

            // 4. Quick Actions Bar: Connectors, HUD, Charts, and Add Widget Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedConnectionType == ConnectionType.BLUETOOTH_CLASSIC,
                                onClick = { viewModel.onIntent(DashboardIntent.SetConnectionType(ConnectionType.BLUETOOTH_CLASSIC)) },
                                label = { Text(stringResource(R.string.connection_bt), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedConnectionType == ConnectionType.BLUETOOTH_LE,
                                onClick = { viewModel.onIntent(DashboardIntent.SetConnectionType(ConnectionType.BLUETOOTH_LE)) },
                                label = { Text(stringResource(R.string.connection_ble), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedConnectionType == ConnectionType.WIFI_TCP,
                                onClick = { viewModel.onIntent(DashboardIntent.SetConnectionType(ConnectionType.WIFI_TCP)) },
                                label = { Text(stringResource(R.string.connection_wifi), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedConnectionType == ConnectionType.USB_SERIAL,
                                onClick = { viewModel.onIntent(DashboardIntent.SetConnectionType(ConnectionType.USB_SERIAL)) },
                                label = { Text(stringResource(R.string.connection_usb), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedConnectionType == ConnectionType.VIRTUAL_SIMULATOR,
                                onClick = { viewModel.onIntent(DashboardIntent.SetConnectionType(ConnectionType.VIRTUAL_SIMULATOR)) },
                                label = { Text(stringResource(R.string.connection_simulator), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonAmber
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Add Custom Widget Button
                        IconButton(
                            onClick = { viewModel.onIntent(DashboardIntent.OpenSensorPicker) },
                            modifier = Modifier.background(DarkCardBg, RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Add,                            contentDescription = stringResource(R.string.cd_add_widget), tint = NeonCyan)
                        }

                        // HUD Mirror mode toggle
                        IconButton(
                            onClick = { viewModel.onIntent(DashboardIntent.ToggleHudMode) },
                            modifier = Modifier.background(DarkCardBg, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = stringResource(R.string.cd_hud),
                                tint = if (state.isHudMode) NeonAmber else TextMuted
                            )
                        }
                    }
                }
            }

            // 5. Dynamically Rendered Customized Widgets
            items(state.widgets, key = { it.id }) { widgetConfig ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    RenderCustomWidget(
                        config = widgetConfig,
                        telemetry = state.telemetry,
                        rpmHistory = state.rpmHistory,
                        speedHistory = state.speedHistory,
                        o2History = state.o2History
                    )

                    // Small remove widget button in top corner
                    IconButton(
                        onClick = { viewModel.onIntent(DashboardIntent.RemoveWidget(widgetConfig.id)) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(26.dp)
                            .padding(4.dp)
                    ) {
                        Icon(Icons.Default.Close,                        contentDescription = stringResource(R.string.cd_remove_widget), tint = TextMuted.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // Modals & Overlays
        if (state.showOnboarding) {
            VehicleOnboardingModal(
                onSetupSaved = { viewModel.onIntent(DashboardIntent.SaveOnboarding(it)) }
            )
        }

        if (state.showSensorPicker) {
            SensorPickerModal(
                onWidgetAdded = { viewModel.onIntent(DashboardIntent.AddWidget(it)) },
                onDismiss = { viewModel.onIntent(DashboardIntent.CloseSensorPicker) }
            )
        }

        if (state.showBluetoothDialog) {
            BluetoothDeviceDialog(
                devices = state.discoveredDevices,
                isScanning = state.isScanningBt,
                onDeviceSelected = { viewModel.onIntent(DashboardIntent.ConnectToDevice(it)) },
                onRefreshScan = { viewModel.onIntent(DashboardIntent.TriggerBtScan) },
                onDismiss = { viewModel.onIntent(DashboardIntent.CloseBluetoothDialog) }
            )
        }
    }
}

@Composable
fun FuelEconomyBanner(
    instantL100km: Float,
    remainingRangeKm: Int,
    costPerKm: Float,
    vehicleName: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(14.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مصرف سوخت و پیمایش ($vehicleName)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = if (instantL100km > 0.05f) String.format(java.util.Locale.US, "%.1f L/100km", instantL100km) else "—",
                    color = NeonAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Route, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("پیمایش باقی‌مانده با سوخت فعلی:", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (remainingRangeKm > 0) "$remainingRangeKm km" else "—", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("هزینه هر کیلومتر:", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (costPerKm > 0f) "${costPerKm.toInt()} ت" else "—", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RenderCustomWidget(
    config: DashboardWidgetConfig,
    telemetry: LiveTelemetry,
    rpmHistory: List<Float>,
    speedHistory: List<Float>,
    o2History: List<Float>
) {
    val sensor = SensorCatalog.findById(config.sensorId)

    val now by telemetryClock()
    val signal = telemetry.signal(sensor.pid, now)
    val sensorValue = signal.value
    if (sensorValue == null) {
        DigitalMetricCard(title = sensor.nameEn, value = "—", unit = sensor.unitMetric, accentColor = TextMuted)
        return
    }
    if (signal.quality == com.sarani.carobd.core.model.SignalQuality.STALE) {
        DigitalMetricCard(title = sensor.nameEn, value = "$sensorValue · " + stringResource(R.string.signal_stale), unit = sensor.unitMetric, accentColor = NeonAmber)
        return
    }

    when (config.widgetType) {
        WidgetType.CIRCULAR_GAUGE -> {
            CircularGauge(
                value = sensorValue,
                minValue = sensor.minValue,
                maxValue = sensor.maxValue,
                title = sensor.nameFa,
                unit = sensor.unitMetric,
                primaryColor = NeonCyan,
                warningColor = NeonRed
            )
        }
        WidgetType.DIGITAL_CARD -> {
            // measured == true (guarded at the top of this function), so a 0f
            // reading is a REAL zero, e.g. throttle at idle — render "0.00",
            // never fabricate "—". (§4 / §8: zero only when the ECU measured 0.)
            val displayValue = if (sensorValue >= 10f) {
                "${sensorValue.toInt()}"
            } else {
                String.format(java.util.Locale.US, "%.2f", sensorValue)
            }
            DigitalMetricCard(
                title = sensor.nameFa,
                value = displayValue,
                unit = sensor.unitMetric,
                accentColor = NeonCyan
            )
        }
        WidgetType.BAR_GAUGE -> {
            HorizontalBarGauge(
                title = sensor.nameFa,
                value = sensorValue,
                minValue = sensor.minValue,
                maxValue = sensor.maxValue,
                unit = sensor.unitMetric,
                accentColor = NeonAmber
            )
        }
        WidgetType.OSCILLOSCOPE_GRAPH -> {
            val history = when (sensor.id) {
                "rpm" -> rpmHistory
                "speed" -> speedHistory
                else -> o2History
            }
            LiveSensorChart(
                title = "${sensor.nameFa} (${sensor.nameEn})",
                history = history,
                unit = sensor.unitMetric,
                accentColor = NeonCyan
            )
        }
    }
}

/**
 * §4 compact primary metrics: Speed / RPM / Coolant / Battery.
 * Unknown (not yet measured) renders "—"; never fabricate a zero value from defaults.
 */
@Composable
fun PrimaryMetricsRow(
    telemetry: LiveTelemetry,
    modifier: Modifier = Modifier
) {
    val now by telemetryClock()
    val items = listOf(
        MetricItem(stringResource(R.string.dashboard_metric_speed), "010D", telemetry.speedKmh, "km/h"),
        MetricItem(stringResource(R.string.dashboard_metric_rpm), "010C", telemetry.rpm, "RPM"),
        MetricItem(stringResource(R.string.dashboard_metric_coolant), "0105", telemetry.coolantTempCelsius, "°C"),
        MetricItem(stringResource(R.string.dashboard_metric_battery), "ATRV", telemetry.batteryVoltage, "V")
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val signal = telemetry.signal(item.pid, now)
            val measured = signal.quality == com.sarani.carobd.core.model.SignalQuality.GOOD
            val display = signal.value?.toString()?.let { if (signal.quality == com.sarani.carobd.core.model.SignalQuality.STALE) it + " · " + stringResource(R.string.signal_stale) else it } ?: "—"
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.label, color = TextMuted, fontSize = 10.sp)
                Text(
                    text = display,
                    color = if (measured) TextPrimary else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(item.unit, color = TextMuted, fontSize = 9.sp)
            }
        }
    }
}

private data class MetricItem(val label: String, val pid: String, val value: Any, val unit: String)

/**
 * §4 Canonical first-screen primary metric PID set, in display order:
 * Speed (010D) → RPM (010C) → Coolant (0105) → Adapter supply (ATRV). These four are
 * rendered compactly by [PrimaryMetricsRow] and are NOT also emitted as
 * dashboard CIRCULAR_GAUGE widgets — this removes the duplicate 132dp circular
 * gauges §4 calls out. Pure top-level fn so the order is JVM-unit-testable.
 */
fun primaryMetricPids(): List<String> = listOf("010D", "010C", "0105", "ATRV")

@Composable
private fun telemetryClock(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) { kotlinx.coroutines.delay(1000); value = System.currentTimeMillis() }
}
