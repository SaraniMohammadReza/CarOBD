package com.sarani.carobd.ui.screens.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ProtocolMode
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.core.model.TransportType
import com.sarani.carobd.data.transport.SimulatorScenario
import com.sarani.carobd.ui.components.CarObdPrimaryButton
import com.sarani.carobd.ui.components.CarObdSecondaryButton
import com.sarani.carobd.ui.components.SectionHeader
import com.sarani.carobd.ui.components.StatusChip
import com.sarani.carobd.ui.components.StatusChipType
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.NeonRed
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionCenterScreen(
    viewModel: ConnectionCenterViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val newConnectionName = stringResource(R.string.qa_new_connection)
    var editing by remember { mutableStateOf<ConnectionProfile?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect {
            if (it is ConnectionCenterEffect.ShowMessage) {
                snackbar.showSnackbar(it.text)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_connection_center),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    onBack?.let {
                        TextButton(onClick = it) {
                            Text(stringResource(R.string.ui_back), color = NeonCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCardBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Prominent Safety Guarantee Reassurance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.connection_safety_title),
                            color = NeonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.connection_safety_desc),
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 2. Active Connection Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ui_connection_state),
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        val chipText: String
                        val chipType: StatusChipType
                        when (val cs = state.connectionState) {
                            is ConnectionState.Connected -> {
                                if (cs.isSimulator) {
                                    chipText = stringResource(R.string.status_connected_sim)
                                    chipType = StatusChipType.WARNING
                                } else {
                                    chipText = stringResource(R.string.status_connected)
                                    chipType = StatusChipType.SUCCESS
                                }
                            }
                            is ConnectionState.Failure -> {
                                chipText = com.sarani.carobd.ui.components.connectionIssueLabel(cs.issue)
                                chipType = StatusChipType.CRITICAL
                            }
                            ConnectionState.Idle, ConnectionState.Disconnected -> {
                                chipText = stringResource(R.string.status_disconnected)
                                chipType = StatusChipType.NEUTRAL
                            }
                            else -> {
                                chipText = stringResource(R.string.status_connecting)
                                chipType = StatusChipType.WARNING
                            }
                        }
                        StatusChip(label = chipText, type = chipType)
                    }

                    when (val cs = state.connectionState) {
                        is ConnectionState.Connected -> {
                            Text(
                                text = if (cs.isSimulator) {
                                    stringResource(R.string.s5_connected_sim)
                                } else {
                                    stringResource(R.string.s5_connected_physical, cs.protocolName)
                                },
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { viewModel.onIntent(ConnectionCenterIntent.Disconnect) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.2f), contentColor = NeonRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_disconnect), fontWeight = FontWeight.Bold)
                            }
                        }
                        is ConnectionState.Failure -> {
                            cs.technicalMessage?.let {
                                Text(it, color = NeonRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        is ConnectionState.TransportConnecting,
                        is ConnectionState.TransportConnected,
                        is ConnectionState.AdapterResetting,
                        is ConnectionState.AdapterIdentifying,
                        is ConnectionState.AdapterConfiguring,
                        is ConnectionState.ProtocolDetecting,
                        is ConnectionState.EcuDiscovering,
                        is ConnectionState.CapabilityDiscovering -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = NeonAmber,
                                    trackColor = DarkBg
                                )
                                val stageDesc = when (cs) {
                                    is ConnectionState.AdapterIdentifying -> "${stringResource(R.string.connection_stage_probe)}: ${cs.probeStage}"
                                    is ConnectionState.AdapterConfiguring -> "${stringResource(R.string.connection_stage_probe)}: ${cs.setting}"
                                    is ConnectionState.ProtocolDetecting -> "${stringResource(R.string.connection_stage_protocol)}: ${cs.candidateName} (${cs.attemptIndex}/${cs.totalCandidates})"
                                    is ConnectionState.EcuDiscovering -> stringResource(R.string.connection_stage_ecu)
                                    else -> stringResource(R.string.connection_stage_transport)
                                }
                                Text(stageDesc, color = NeonAmber, fontSize = 11.sp)
                                OutlinedButton(
                                    onClick = { viewModel.onIntent(ConnectionCenterIntent.Disconnect) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.ui_cancel))
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = stringResource(R.string.s5_disconnected),
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 3. Diagnostic Test Report (if available)
            state.testReport?.let { report ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, if (report.passed) NeonGreen else NeonAmber, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (report.isSimulator) stringResource(R.string.s5_sim_evidence) else stringResource(R.string.s5_physical_evidence),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        report.stages.forEach { stage ->
                            Text(
                                text = "${stage.stage}: ${stage.status} · ${stage.evidence}",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (report.passed) {
                            Text(stringResource(R.string.ui_connection_passed), color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Saved Connection Profiles Section
            SectionHeader(
                title = stringResource(R.string.connection_profiles_header),
                actionLabel = stringResource(R.string.connection_add_adapter_btn),
                onActionClick = {
                    editing = ConnectionProfile.newProfile(
                        newConnectionName,
                        TransportConfig.BluetoothClassic("")
                    )
                }
            )

            if (state.profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.ui_no_profiles), color = TextMuted, fontSize = 12.sp)
                        CarObdPrimaryButton(
                            text = stringResource(R.string.connection_add_adapter_btn),
                            onClick = {
                                editing = ConnectionProfile.newProfile(
                                    newConnectionName,
                                    TransportConfig.BluetoothClassic("")
                                )
                            }
                        )
                    }
                }
            }

            state.profiles.forEach { profile ->
                val isSelected = profile.id == state.selectedProfileId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan.copy(alpha = 0.6f) else DarkCardBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.s5_selected),
                                        color = NeonCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = profile.transportType,
                                color = NeonAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = stringResource(R.string.connection_profile_details, profile.target, profile.protocol),
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                enabled = !state.isConnecting,
                                onClick = { viewModel.onIntent(ConnectionCenterIntent.ConnectSpecific(profile.id)) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.ui_connect), color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                enabled = !state.isConnecting,
                                onClick = { viewModel.onIntent(ConnectionCenterIntent.TestConnection(profile.id)) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonAmber)
                            ) {
                                Text(stringResource(R.string.ui_test), fontSize = 11.sp)
                            }
                            IconButton(
                                onClick = { editing = state.fullProfiles.find { it.id == profile.id } },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.ui_edit), tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.onIntent(ConnectionCenterIntent.Delete(profile.id)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_delete), tint = NeonRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    editing?.let { profile ->
        ProfileEditor(
            profile = profile,
            onDismiss = { editing = null },
            onSave = {
                viewModel.onIntent(ConnectionCenterIntent.SaveProfile(it))
                editing = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditor(
    profile: ConnectionProfile,
    onDismiss: () -> Unit,
    onSave: (ConnectionProfile) -> Unit
) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var type by remember(profile.id) { mutableStateOf(profile.transport.type) }
    var address by remember(profile.id) {
        mutableStateOf(
            when (val t = profile.transport) {
                is TransportConfig.BluetoothClassic -> t.deviceAddress
                is TransportConfig.Ble -> t.deviceAddress
                else -> ""
            }
        )
    }
    var host by remember(profile.id) {
        mutableStateOf((profile.transport as? TransportConfig.Wifi)?.host?.ifBlank { "192.168.0.10" } ?: "192.168.0.10")
    }
    var port by remember(profile.id) {
        mutableStateOf((profile.transport as? TransportConfig.Wifi)?.port?.toString() ?: "35000")
    }
    val usb = profile.transport as? TransportConfig.Usb
    var deviceId by remember(profile.id) { mutableStateOf(usb?.deviceId?.toString().orEmpty()) }
    var vid by remember(profile.id) { mutableStateOf(usb?.vendorId?.toString().orEmpty()) }
    var pid by remember(profile.id) { mutableStateOf(usb?.productId?.toString().orEmpty()) }
    var usbPort by remember(profile.id) { mutableStateOf(usb?.portIndex?.toString().orEmpty()) }
    var baud by remember(profile.id) { mutableStateOf(usb?.baudRate?.toString() ?: "38400") }
    var scenario by remember(profile.id) {
        mutableStateOf((profile.transport as? TransportConfig.Simulator)?.scenarioId ?: "healthy")
    }
    var manual by remember(profile.id) {
        mutableStateOf(if (profile.protocolMode == ProtocolMode.MANUAL) profile.manualProtocolCode.orEmpty() else "")
    }
    var connectTimeout by remember(profile.id) { mutableStateOf(profile.connectTimeoutMs.toString()) }
    var commandTimeout by remember(profile.id) { mutableStateOf(profile.commandTimeoutMs.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_profile), color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.ui_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.ui_connection_title),
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TransportType.entries.forEach { choice ->
                        FilterChip(
                            selected = type == choice,
                            onClick = { type = choice },
                            label = {
                                Text(
                                    stringResource(
                                        when (choice) {
                                            TransportType.BLUETOOTH_CLASSIC -> R.string.connection_bt
                                            TransportType.BLUETOOTH_LE -> R.string.connection_ble
                                            TransportType.WIFI_TCP -> R.string.connection_wifi
                                            TransportType.USB_SERIAL -> R.string.connection_usb
                                            TransportType.SIMULATOR -> R.string.connection_simulator
                                        }
                                    ),
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }

                // Educational transport hint
                val hint = when (type) {
                    TransportType.BLUETOOTH_CLASSIC -> stringResource(R.string.connection_transport_hint_bt)
                    TransportType.BLUETOOTH_LE -> stringResource(R.string.connection_transport_hint_ble)
                    TransportType.WIFI_TCP -> stringResource(R.string.connection_transport_hint_wifi)
                    TransportType.USB_SERIAL -> stringResource(R.string.connection_transport_hint_usb)
                    TransportType.SIMULATOR -> stringResource(R.string.connection_transport_hint_sim)
                }
                Text(hint, color = NeonAmber, fontSize = 11.sp, lineHeight = 15.sp)

                when (type) {
                    TransportType.BLUETOOTH_CLASSIC, TransportType.BLUETOOTH_LE -> {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(stringResource(R.string.ui_mac)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        com.sarani.carobd.ui.components.BluetoothTargetPicker(type == TransportType.BLUETOOTH_LE) {
                            address = it
                        }
                    }
                    TransportType.WIFI_TCP -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text(stringResource(R.string.ui_host)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text(stringResource(R.string.ui_tcp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TransportType.USB_SERIAL -> {
                        Text(stringResource(R.string.ui_usb_help), color = TextMuted, fontSize = 11.sp)
                        OutlinedTextField(deviceId, { deviceId = it }, label = { Text(stringResource(R.string.ui_usb_device)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(vid, { vid = it }, label = { Text(stringResource(R.string.ui_usb_vendor)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(pid, { pid = it }, label = { Text(stringResource(R.string.ui_usb_product)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(usbPort, { usbPort = it }, label = { Text(stringResource(R.string.ui_usb_interface)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(baud, { baud = it }, label = { Text(stringResource(R.string.ui_usb_baud)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    TransportType.SIMULATOR -> {
                        SimulatorScenario.entries.forEach { option ->
                            FilterChip(
                                selected = scenario == option.id,
                                onClick = { scenario = option.id },
                                label = {
                                    Text(
                                        if (androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "fa") option.labelFa else option.labelEn,
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = connectTimeout,
                    onValueChange = { connectTimeout = it },
                    label = { Text(stringResource(R.string.ui_connect_timeout)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commandTimeout,
                    onValueChange = { commandTimeout = it },
                    label = { Text(stringResource(R.string.ui_command_timeout)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = manual,
                    onValueChange = { manual = it },
                    label = { Text(stringResource(R.string.ui_manual_protocol)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = runCatching {
                        fun optionalInt(raw: String): Int? = raw.trim().takeIf { it.isNotEmpty() }?.let {
                            if (it.startsWith("0x", true)) it.drop(2).toInt(16) else it.toInt()
                        }
                        val transport = when (type) {
                            TransportType.BLUETOOTH_CLASSIC -> TransportConfig.BluetoothClassic(address.trim())
                            TransportType.BLUETOOTH_LE -> TransportConfig.Ble(address.trim())
                            TransportType.WIFI_TCP -> TransportConfig.Wifi(host.trim(), port.toInt())
                            TransportType.USB_SERIAL -> TransportConfig.Usb(optionalInt(vid), optionalInt(pid), optionalInt(deviceId), optionalInt(usbPort), optionalInt(baud))
                            TransportType.SIMULATOR -> TransportConfig.Simulator(scenario)
                        }
                        profile.copy(
                            name = name.trim(),
                            transport = transport,
                            protocolMode = if (manual.isBlank()) ProtocolMode.AUTO else ProtocolMode.MANUAL,
                            manualProtocolCode = manual.trim().uppercase().takeIf { it.isNotEmpty() },
                            vehicleProfileId = null,
                            autoReconnect = false,
                            connectTimeoutMs = connectTimeout.toLong(),
                            commandTimeoutMs = commandTimeout.toLong(),
                            simulatorScenarioId = null,
                            lastSuccessfulAt = null
                        ).also { require(it.validate().isEmpty()) { it.validate().joinToString("\n") } }
                    }
                    result.onSuccess(onSave).onFailure { error = it.message ?: "Invalid profile" }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(stringResource(R.string.ui_save), color = DarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_cancel), color = TextMuted)
            }
        },
        containerColor = DarkCardBg
    )
}
