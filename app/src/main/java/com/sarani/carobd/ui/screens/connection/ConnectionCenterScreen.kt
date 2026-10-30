package com.sarani.carobd.ui.screens.connection

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sarani.carobd.core.model.*
import com.sarani.carobd.data.transport.SimulatorScenario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionCenterScreen(viewModel: ConnectionCenterViewModel, onBack: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val newConnectionName = stringResource(com.sarani.carobd.R.string.qa_new_connection)
    var editing by remember { mutableStateOf<ConnectionProfile?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { if (it is ConnectionCenterEffect.ShowMessage) snackbar.showSnackbar(it.text) }
    }
    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(title = { Text(stringResource(com.sarani.carobd.R.string.ui_connection_title)) }, navigationIcon = {
            onBack?.let { TextButton(onClick = it) { Text(stringResource(com.sarani.carobd.R.string.ui_back)) } }
        }, actions = {
            TextButton(onClick = { editing = ConnectionProfile.newProfile(newConnectionName, TransportConfig.Simulator("healthy")) }) { Text(stringResource(com.sarani.carobd.R.string.ui_add)) }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.sarani.carobd.ui.components.RuntimeAccessControls()
            Text(when (val connection = state.connectionState) {
                is ConnectionState.Connected -> if (connection.isSimulator) stringResource(com.sarani.carobd.R.string.s5_connected_sim) else stringResource(com.sarani.carobd.R.string.s5_connected_physical, connection.protocolName)
                is ConnectionState.Failure -> com.sarani.carobd.ui.components.connectionIssueLabel(connection.issue)
                ConnectionState.Idle, ConnectionState.Disconnected -> stringResource(com.sarani.carobd.R.string.s5_disconnected)
                else -> stringResource(com.sarani.carobd.R.string.s5_connection_progress)
            })
            (state.connectionState as? ConnectionState.Failure)?.let { com.sarani.carobd.ui.components.DeveloperEvidence(it.technicalMessage) }
            if (state.isConnected || state.isConnecting) {
                Button(onClick = { viewModel.onIntent(ConnectionCenterIntent.Disconnect) }) { Text(stringResource(com.sarani.carobd.R.string.ui_disconnect_cancel)) }
            }
            Text(stringResource(com.sarani.carobd.R.string.ui_connection_safety))
            state.testReport?.let { report ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (report.isSimulator) stringResource(com.sarani.carobd.R.string.s5_sim_evidence) else stringResource(com.sarani.carobd.R.string.s5_physical_evidence))
                        report.stages.forEach { stage -> Text("${stage.stage}: ${stage.status}\n${stage.evidence}") }
                        if (report.passed) Text(stringResource(com.sarani.carobd.R.string.ui_connection_passed))
                    }
                }
            }
            if (state.profiles.isEmpty()) Text(stringResource(com.sarani.carobd.R.string.ui_no_profiles))
            state.profiles.forEach { profile ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(profile.name + if (profile.id == state.selectedProfileId) stringResource(com.sarani.carobd.R.string.s5_selected) else "")
                        Text(stringResource(com.sarani.carobd.R.string.s5_profile_line, profile.transportType, profile.target, profile.protocol))
                        Row {
                            TextButton(enabled = !state.isConnecting, onClick = { viewModel.onIntent(ConnectionCenterIntent.ConnectSpecific(profile.id)) }) { Text(stringResource(com.sarani.carobd.R.string.ui_connect)) }
                            TextButton(enabled = !state.isConnecting, onClick = { viewModel.onIntent(ConnectionCenterIntent.TestConnection(profile.id)) }) { Text(stringResource(com.sarani.carobd.R.string.ui_test)) }
                            TextButton(onClick = { editing = state.fullProfiles.find { it.id == profile.id } }) { Text(stringResource(com.sarani.carobd.R.string.ui_edit)) }
                        }
                        Row {
                            TextButton(onClick = { viewModel.onIntent(ConnectionCenterIntent.Select(profile.id)) }) { Text(stringResource(com.sarani.carobd.R.string.ui_select)) }
                            TextButton(onClick = { viewModel.onIntent(ConnectionCenterIntent.Delete(profile.id)) }) { Text(stringResource(com.sarani.carobd.R.string.ui_delete)) }
                        }
                    }
                }
            }
        }
    }
    editing?.let { profile ->
        ProfileEditor(profile, onDismiss = { editing = null }, onSave = {
            viewModel.onIntent(ConnectionCenterIntent.SaveProfile(it)); editing = null
        })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditor(profile: ConnectionProfile, onDismiss: () -> Unit, onSave: (ConnectionProfile) -> Unit) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var type by remember(profile.id) { mutableStateOf(profile.transport.type) }
    var address by remember(profile.id) { mutableStateOf(when (val t = profile.transport) { is TransportConfig.BluetoothClassic -> t.deviceAddress; is TransportConfig.Ble -> t.deviceAddress; else -> "" }) }
    var host by remember(profile.id) { mutableStateOf((profile.transport as? TransportConfig.Wifi)?.host.orEmpty()) }
    var port by remember(profile.id) { mutableStateOf((profile.transport as? TransportConfig.Wifi)?.port?.toString().orEmpty()) }
    val usb = profile.transport as? TransportConfig.Usb
    var deviceId by remember(profile.id) { mutableStateOf(usb?.deviceId?.toString().orEmpty()) }
    var vid by remember(profile.id) { mutableStateOf(usb?.vendorId?.toString().orEmpty()) }
    var pid by remember(profile.id) { mutableStateOf(usb?.productId?.toString().orEmpty()) }
    var usbPort by remember(profile.id) { mutableStateOf(usb?.portIndex?.toString().orEmpty()) }
    var baud by remember(profile.id) { mutableStateOf(usb?.baudRate?.toString().orEmpty()) }
    var scenario by remember(profile.id) { mutableStateOf((profile.transport as? TransportConfig.Simulator)?.scenarioId ?: "healthy") }
    var manual by remember(profile.id) { mutableStateOf(if (profile.protocolMode == ProtocolMode.MANUAL) profile.manualProtocolCode.orEmpty() else "") }
    var connectTimeout by remember(profile.id) { mutableStateOf(profile.connectTimeoutMs.toString()) }
    var commandTimeout by remember(profile.id) { mutableStateOf(profile.commandTimeoutMs.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(com.sarani.carobd.R.string.ui_profile)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_name)) }, singleLine = true)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransportType.entries.forEach { choice ->
                FilterChip(selected = type == choice, onClick = { type = choice }, label = { Text(stringResource(when (choice) {
                    TransportType.BLUETOOTH_CLASSIC -> com.sarani.carobd.R.string.connection_bt
                    TransportType.BLUETOOTH_LE -> com.sarani.carobd.R.string.connection_ble
                    TransportType.WIFI_TCP -> com.sarani.carobd.R.string.connection_wifi
                    TransportType.USB_SERIAL -> com.sarani.carobd.R.string.connection_usb
                    TransportType.SIMULATOR -> com.sarani.carobd.R.string.connection_simulator
                })) })
            }
            }
            when (type) {
                TransportType.BLUETOOTH_CLASSIC, TransportType.BLUETOOTH_LE -> {
                    OutlinedTextField(address, { address = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_mac)) }, singleLine = true)
                    com.sarani.carobd.ui.components.BluetoothTargetPicker(type == TransportType.BLUETOOTH_LE) { address = it }
                }
                TransportType.WIFI_TCP -> {
                    OutlinedTextField(host, { host = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_host)) }, singleLine = true)
                    OutlinedTextField(port, { port = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_tcp)) }, singleLine = true)
                }
                TransportType.USB_SERIAL -> {
                    Text(stringResource(com.sarani.carobd.R.string.ui_usb_help))
                    OutlinedTextField(deviceId, { deviceId = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_usb_device)) }, singleLine = true)
                    OutlinedTextField(vid, { vid = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_usb_vendor)) }, singleLine = true)
                    OutlinedTextField(pid, { pid = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_usb_product)) }, singleLine = true)
                    OutlinedTextField(usbPort, { usbPort = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_usb_interface)) }, singleLine = true)
                    OutlinedTextField(baud, { baud = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_usb_baud)) }, singleLine = true)
                }
                TransportType.SIMULATOR -> SimulatorScenario.entries.forEach { option ->
                    FilterChip(selected = scenario == option.id, onClick = { scenario = option.id }, label = { Text(if (androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "fa") option.labelFa else option.labelEn) })
                }
            }
            OutlinedTextField(connectTimeout, { connectTimeout = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_connect_timeout)) }, singleLine = true)
            OutlinedTextField(commandTimeout, { commandTimeout = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_command_timeout)) }, singleLine = true)
            Text(stringResource(com.sarani.carobd.R.string.ui_reconnect_unavailable))
            OutlinedTextField(manual, { manual = it }, label = { Text(stringResource(com.sarani.carobd.R.string.ui_manual_protocol)) }, singleLine = true)
            Text(stringResource(com.sarani.carobd.R.string.ui_ble_limits))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(onClick = {
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
                profile.copy(name = name.trim(), transport = transport, protocolMode = if (manual.isBlank()) ProtocolMode.AUTO else ProtocolMode.MANUAL,
                    manualProtocolCode = manual.trim().uppercase().takeIf { it.isNotEmpty() }, vehicleProfileId = null,
                    autoReconnect = false, connectTimeoutMs = connectTimeout.toLong(), commandTimeoutMs = commandTimeout.toLong(), simulatorScenarioId = null, lastSuccessfulAt = null).also { require(it.validate().isEmpty()) { it.validate().joinToString("\n") } }
            }
            result.onSuccess(onSave).onFailure { error = it.message ?: "Invalid profile" }
        }) { Text(stringResource(com.sarani.carobd.R.string.ui_save)) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(com.sarani.carobd.R.string.ui_cancel)) } })
}
