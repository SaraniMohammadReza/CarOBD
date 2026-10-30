package com.sarani.carobd.ui.screens.adapter

import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.core.database.MotorcycleAndChineseSpecs
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.domain.usecase.ObserveTelemetryUseCase
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.NeonRed
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary
import org.koin.compose.koinInject

@Composable
fun AdapterInspectorScreen(
    modifier: Modifier = Modifier,
    observeTelemetryUseCase: ObserveTelemetryUseCase = koinInject()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val connectionState by observeTelemetryUseCase.connectionState.collectAsState()
    val telemetry by observeTelemetryUseCase.telemetry.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(com.sarani.carobd.R.string.ui_adapter_title),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(com.sarani.carobd.R.string.ui_adapter_intro),
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkCardBg,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(com.sarani.carobd.R.string.ui_adapter_tab), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) NeonCyan else TextMuted) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(com.sarani.carobd.R.string.ui_troubleshoot_tab), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) NeonCyan else TextMuted) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(com.sarani.carobd.R.string.ui_motorcycle_tab), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 2) NeonCyan else TextMuted) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> AdapterHealthView(connectionState = connectionState, telemetry = telemetry)
                1 -> TroubleshootingGuideView()
                2 -> MotorcyclePinoutView()
            }
        }
    }
}

@Composable
fun AdapterHealthView(
    connectionState: ConnectionState,
    telemetry: LiveTelemetry
) {
    val isConnected = connectionState is ConnectionState.Connected
    val connectedInfo = connectionState as? ConnectionState.Connected

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val borderColor = if (isConnected) NeonGreen.copy(alpha = 0.5f) else DarkCardBorder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.LinkOff,
                            contentDescription = null,
                            tint = if (isConnected) NeonGreen else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) {
                                stringResource(com.sarani.carobd.R.string.s5_adapter_connected, connectedInfo?.deviceName ?: "—")
                            } else {
                                stringResource(com.sarani.carobd.R.string.s5_no_adapter)
                            },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isConnected) {
                            stringResource(com.sarani.carobd.R.string.s5_reported_firmware, connectedInfo?.elmVersion ?: "—")
                        } else {
                            stringResource(com.sarani.carobd.R.string.s5_adapter_identity_limit)
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(com.sarani.carobd.R.string.ui_adapter_fields), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    HealthRow(
                        label = stringResource(com.sarani.carobd.R.string.ui_connection_state),
                        value = if (isConnected) stringResource(com.sarani.carobd.R.string.s5_connected) else stringResource(com.sarani.carobd.R.string.s5_disconnected)
                    )
                    HealthRow(
                        label = stringResource(com.sarani.carobd.R.string.ui_observed_protocol),
                        value = if (isConnected) (connectedInfo?.protocolName ?: "—") else "—"
                    )
                    HealthRow(
                        label = stringResource(com.sarani.carobd.R.string.ui_reported_firmware),
                        value = if (isConnected) (connectedInfo?.elmVersion ?: "—") else "—"
                    )
                    HealthRow(
                        label = stringResource(com.sarani.carobd.R.string.ui_adapter_voltage),
                        value = if (isConnected && telemetry.signal("ATRV").quality == com.sarani.carobd.core.model.SignalQuality.GOOD) {
                            String.format(java.util.Locale.US, "%.1f V", telemetry.batteryVoltage)
                        } else "—"
                    )
                    HealthRow(
                        label = stringResource(com.sarani.carobd.R.string.ui_latency),
                        value = if (isConnected) stringResource(com.sarani.carobd.R.string.s5_latency_unknown) else "—"
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(com.sarani.carobd.R.string.ui_adapter_limit_title), color = NeonAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(com.sarani.carobd.R.string.ui_adapter_limit_body),
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TroubleshootingGuideView() {
    val issues = listOf(
        Pair(stringResource(com.sarani.carobd.R.string.s5_help_power), stringResource(com.sarani.carobd.R.string.s5_help_power_body)),
        Pair(stringResource(com.sarani.carobd.R.string.s5_help_init), stringResource(com.sarani.carobd.R.string.s5_help_init_body)),
        Pair(stringResource(com.sarani.carobd.R.string.s5_help_pair), stringResource(com.sarani.carobd.R.string.s5_help_pair_body)),
        Pair(stringResource(com.sarani.carobd.R.string.s5_help_loss), stringResource(com.sarani.carobd.R.string.s5_help_loss_body))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(issues) { (title, solution) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Help, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(solution, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun MotorcyclePinoutView() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(com.sarani.carobd.R.string.ui_motorcycle_limits),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        items(MotorcycleAndChineseSpecs.MOTORCYCLE_PINOUTS) { moto ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(moto.brandName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(com.sarani.carobd.R.string.s5_connector, moto.connectorType), color = NeonCyan, fontSize = 11.sp)
                    Text(stringResource(com.sarani.carobd.R.string.s5_models, moto.supportedModelsFa), color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    moto.pinoutDescriptionFa.forEach { pin ->
                        Text("• $pin", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HealthRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(value, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
