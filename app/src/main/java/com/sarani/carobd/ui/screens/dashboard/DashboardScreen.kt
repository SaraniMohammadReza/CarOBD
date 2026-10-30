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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.filled.OpenInFull
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
    val moveUpLabel = stringResource(R.string.widget_move_up)
    val moveDownLabel = stringResource(R.string.widget_move_down)
    val resizeLabel = stringResource(R.string.widget_resize)
    val state by viewModel.uiState.collectAsState()
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner, viewModel) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) viewModel.stopDiscovery() }
        owner.lifecycle.addObserver(observer)
        onDispose { viewModel.stopDiscovery(); owner.lifecycle.removeObserver(observer) }
    }
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
            item { Text(com.sarani.carobd.ui.components.recordingLabel(recording)); Text(state.activeConnectionName ?: state.connectingName ?: "—") }
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
                val fuelEvidence = com.sarani.carobd.core.calculator.FuelEconomyEngine.calculate(state.telemetry,
                    state.userVehicleSetup.tankCapacityLiters, state.userVehicleSetup.fuelPriceTomans, nowMs = currentTime)
                FuelEconomyBanner(
                    instantL100km = fuelEvidence.instantLitersPer100Km,
                    remainingRangeKm = fuelEvidence.remainingRangeKm,
                    costPerKm = fuelEvidence.fuelCostPerKmTomans,
                    vehicleName = state.activeProfile?.let { if (androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "fa") it.modelNameFa else it.modelNameEn } ?: stringResource(R.string.s5_unknown)
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

            if (state.layoutSaveFailed) item { Text(stringResource(R.string.widget_save_failed), color = NeonAmber) }
            // 5. Dynamically Rendered Customized Widgets
            items(state.widgets, key = { it.id }) { widgetConfig ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    RenderCustomWidget(
                        config = widgetConfig,
                        telemetry = state.telemetry,
                        graphHistory = state.graphHistory,
                        modifier = Modifier.fillMaxWidth(if (widgetConfig.widgetSize == WidgetSize.LARGE) 1f else 0.5f)
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.onIntent(DashboardIntent.MoveWidget(widgetConfig.id, -1)) }) { Text("↑", modifier = Modifier.semantics { contentDescription = moveUpLabel }) }
                        TextButton(onClick = { viewModel.onIntent(DashboardIntent.MoveWidget(widgetConfig.id, 1)) }) { Text("↓", modifier = Modifier.semantics { contentDescription = moveDownLabel }) }
                        IconButton(onClick = { viewModel.onIntent(DashboardIntent.ResizeWidget(widgetConfig.id)) }) {
                            Icon(Icons.Default.OpenInFull, contentDescription = resizeLabel)
                        }
                        IconButton(onClick = { viewModel.onIntent(DashboardIntent.RemoveWidget(widgetConfig.id)) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove_widget))
                        }
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
                telemetry = state.telemetry,
                onWidgetAdded = { viewModel.onIntent(DashboardIntent.AddWidget(it)) },
                onDismiss = { viewModel.onIntent(DashboardIntent.CloseSensorPicker) }
            )
        }

        if (state.showBluetoothDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(DashboardIntent.CloseBluetoothDialog) },
                title = { Text(stringResource(R.string.ui_bt_pick)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        com.sarani.carobd.ui.components.BluetoothTargetPicker(
                            ble = state.selectedConnectionType == ConnectionType.BLUETOOTH_LE
                        ) { address ->
                            viewModel.onIntent(DashboardIntent.ConnectToDevice(
                                com.sarani.carobd.core.connectivity.DiscoveredBluetoothDevice("", address, false, false)
                            ))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { viewModel.onIntent(DashboardIntent.CloseBluetoothDialog) }) { Text(stringResource(R.string.ui_cancel)) } }
            )
        }
    }
}

@Composable
fun FuelEconomyBanner(
    instantL100km: Float?,
    remainingRangeKm: Int?,
    costPerKm: Float?,
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
                    Text(stringResource(com.sarani.carobd.R.string.s5_fuel_title, vehicleName), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = if (instantL100km != null) com.sarani.carobd.ui.theme.displayMeasurement(instantL100km.toDouble(), "L/100km", com.sarani.carobd.ui.theme.LocalUnitSystem.current).let { "%.1f %s".format(java.util.Locale.US, it.value, it.unit) } else "—",
                    color = NeonAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(stringResource(com.sarani.carobd.R.string.s5_fuel_assumptions), color = TextMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Route, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(com.sarani.carobd.R.string.s5_range), color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (remainingRangeKm != null) com.sarani.carobd.core.calculator.UnitConverter.formatDistance(remainingRangeKm.toDouble(), com.sarani.carobd.ui.theme.LocalUnitSystem.current).let { "${it.first} ${it.second}" } else "—", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(com.sarani.carobd.R.string.s5_cost), color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (costPerKm != null) "%.0f toman/%s".format(java.util.Locale.US, costPerKm * if (com.sarani.carobd.ui.theme.LocalUnitSystem.current == com.sarani.carobd.core.storage.UnitSystem.IMPERIAL) 1.609344 else 1.0, if (com.sarani.carobd.ui.theme.LocalUnitSystem.current == com.sarani.carobd.core.storage.UnitSystem.IMPERIAL) "mi" else "km") else "—", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RenderCustomWidget(
    config: DashboardWidgetConfig,
    telemetry: LiveTelemetry,
    graphHistory: Map<String, List<com.sarani.carobd.core.model.TelemetryGraphPoint>>,
    modifier: Modifier = Modifier
) {
    val sensor = SensorCatalog.findById(config.sensorId)
    if (sensor == null) {
        Text(stringResource(R.string.sensor_not_polled), modifier = modifier)
        return
    }

    Box(modifier) {
        val now by telemetryClock()
        val signal = telemetry.signal(sensor.pid, now)
        val rawValue = signal.value
        val units = com.sarani.carobd.ui.theme.LocalUnitSystem.current
        val display = rawValue?.let { com.sarani.carobd.ui.theme.displayMeasurement(it.toDouble(), sensor.unitMetric, units) }
        val sensorValue = display?.value?.toFloat()
        val displayUnit = display?.unit ?: sensor.unitMetric
        val title = if (androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "fa") sensor.nameFa else sensor.nameEn
        if (config.widgetType == WidgetType.OSCILLOSCOPE_GRAPH) {
            LiveSensorChart(title = title,
                history = graphHistory[sensor.pid].orEmpty().map { it.copy(value = it.value?.let { value -> com.sarani.carobd.ui.theme.displayMeasurement(value.toDouble(), sensor.unitMetric, units).value.toFloat() }) },
                currentValue = sensorValue.takeIf { signal.quality == com.sarani.carobd.core.model.SignalQuality.GOOD },
                quality = signal.quality,
                unit = displayUnit)
            return
        }
        if (sensorValue == null) {
            DigitalMetricCard(title = title, value = "—", unit = displayUnit, accentColor = TextMuted)
            return
        }
        if (signal.quality == com.sarani.carobd.core.model.SignalQuality.STALE) {
            DigitalMetricCard(title = title, value = String.format(java.util.Locale.US, "%.2f", sensorValue), unit = displayUnit, accentColor = NeonAmber, status = stringResource(R.string.signal_stale))
            return
        }
        val minimum = com.sarani.carobd.ui.theme.displayMeasurement(sensor.minValue.toDouble(), sensor.unitMetric, units).value.toFloat()
        val maximum = com.sarani.carobd.ui.theme.displayMeasurement(sensor.maxValue.toDouble(), sensor.unitMetric, units).value.toFloat()

        when (config.widgetType) {
            WidgetType.CIRCULAR_GAUGE -> {
                CircularGauge(
                    value = sensorValue,
                    minValue = minimum,
                    maxValue = maximum,
                    title = title,
                    unit = displayUnit,
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
                    title = title,
                    value = displayValue,
                    unit = displayUnit,
                    accentColor = NeonCyan
                )
            }
            WidgetType.BAR_GAUGE -> {
                HorizontalBarGauge(
                    title = title,
                    value = sensorValue,
                    minValue = minimum,
                    maxValue = maximum,
                    unit = displayUnit,
                    accentColor = NeonAmber
                )
            }
            WidgetType.OSCILLOSCOPE_GRAPH -> Unit
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { item ->
                    val signal = telemetry.signal(item.pid, now)
                    val converted = signal.value?.let { com.sarani.carobd.ui.theme.displayMeasurement(it.toDouble(), item.unit, com.sarani.carobd.ui.theme.LocalUnitSystem.current) }
                    val display = converted?.value?.let { String.format(java.util.Locale.US, if (item.pid in listOf("010D", "010C", "0105")) "%.0f" else "%.1f", it) } ?: "—"
                    androidx.compose.material3.Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.label, color = TextMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                            com.sarani.carobd.ui.components.DiagnosticIdentifier(display + " " + (converted?.unit ?: item.unit))
                            if (signal.quality == com.sarani.carobd.core.model.SignalQuality.STALE) Text(stringResource(R.string.signal_stale), color = NeonAmber)
                        }
                    }
                }
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
