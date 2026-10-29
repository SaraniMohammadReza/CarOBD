package com.sarani.carobd.ui.screens.connection

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
    var editing by remember { mutableStateOf<ConnectionProfile?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { if (it is ConnectionCenterEffect.ShowMessage) snackbar.showSnackbar(it.text) }
    }
    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(title = { Text("Read-only connections") }, navigationIcon = {
            onBack?.let { TextButton(onClick = it) { Text("Back") } }
        }, actions = {
            TextButton(onClick = { editing = ConnectionProfile.newProfile("New connection", TransportConfig.Simulator("healthy")) }) { Text("Add") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.sarani.carobd.ui.components.RuntimeAccessControls()
            Text(when (val connection = state.connectionState) {
                is ConnectionState.Connected -> if (connection.isSimulator) "Connected — SIMULATOR" else "Connected — physical ECU read-only (${connection.protocolName})"
                is ConnectionState.Failure -> "Failed: ${connection.technicalMessage}"
                else -> connection.toString()
            })
            if (state.isConnected || state.isConnecting) {
                Button(onClick = { viewModel.onIntent(ConnectionCenterIntent.Disconnect) }) { Text("Disconnect / cancel") }
            }
            Text("Physical clearing, actuator control and programming are disabled. Test connection takes session ownership and closes the test link when finished.")
            state.testReport?.let { report ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (report.isSimulator) "SIMULATOR test evidence" else "Physical read-only test evidence")
                        report.stages.forEach { stage -> Text("${stage.stage}: ${stage.status}\n${stage.evidence}") }
                        if (report.passed) Text("Read-only test passed; test session closed. Device/vehicle validation remains required.")
                    }
                }
            }
            if (state.profiles.isEmpty()) Text("No profiles. Add a profile with an explicit target.")
            state.profiles.forEach { profile ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(profile.name + if (profile.id == state.selectedProfileId) " (selected)" else "")
                        Text("${profile.transportType}: ${profile.target}\nProtocol: ${profile.protocol}")
                        Row {
                            TextButton(enabled = !state.isConnecting, onClick = { viewModel.onIntent(ConnectionCenterIntent.ConnectSpecific(profile.id)) }) { Text("Connect") }
                            TextButton(enabled = !state.isConnecting, onClick = { viewModel.onIntent(ConnectionCenterIntent.TestConnection(profile.id)) }) { Text("Test") }
                            TextButton(onClick = { editing = state.fullProfiles.find { it.id == profile.id } }) { Text("Edit") }
                        }
                        Row {
                            TextButton(onClick = { viewModel.onIntent(ConnectionCenterIntent.Select(profile.id)) }) { Text("Select") }
                            TextButton(onClick = { viewModel.onIntent(ConnectionCenterIntent.Delete(profile.id)) }) { Text("Delete") }
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
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Connection profile") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
            TransportType.entries.forEach { choice ->
                FilterChip(selected = type == choice, onClick = { type = choice }, label = { Text(choice.name) })
            }
            when (type) {
                TransportType.BLUETOOTH_CLASSIC, TransportType.BLUETOOTH_LE -> {
                    OutlinedTextField(address, { address = it }, label = { Text("Exact device MAC address") }, singleLine = true)
                    com.sarani.carobd.ui.components.BluetoothTargetPicker(type == TransportType.BLUETOOTH_LE) { address = it }
                }
                TransportType.WIFI_TCP -> {
                    OutlinedTextField(host, { host = it }, label = { Text("Host / IP address") }, singleLine = true)
                    OutlinedTextField(port, { port = it }, label = { Text("TCP port (required)") }, singleLine = true)
                }
                TransportType.USB_SERIAL -> {
                    Text("Choose device ID or both VID and PID. Interface is required. IDs use decimal; 0x hexadecimal is also accepted. No fallback device is used.")
                    OutlinedTextField(deviceId, { deviceId = it }, label = { Text("Device ID (optional with VID/PID)") }, singleLine = true)
                    OutlinedTextField(vid, { vid = it }, label = { Text("Vendor ID") }, singleLine = true)
                    OutlinedTextField(pid, { pid = it }, label = { Text("Product ID") }, singleLine = true)
                    OutlinedTextField(usbPort, { usbPort = it }, label = { Text("Interface index (required)") }, singleLine = true)
                    OutlinedTextField(baud, { baud = it }, label = { Text("CDC baud (blank = 38400)") }, singleLine = true)
                }
                TransportType.SIMULATOR -> SimulatorScenario.entries.forEach { option ->
                    FilterChip(selected = scenario == option.id, onClick = { scenario = option.id }, label = { Text(option.labelEn) })
                }
            }
            OutlinedTextField(manual, { manual = it }, label = { Text("Manual protocol 0–9/A; blank = AUTO") }, singleLine = true)
            Text("BLE uses supported NUS/FFF0 services. Custom UUID/MTU and vehicle-profile protocol selection are unavailable.")
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
                    autoReconnect = false, simulatorScenarioId = null, lastSuccessfulAt = null).also { require(it.validate().isEmpty()) { it.validate().joinToString("\n") } }
            }
            result.onSuccess(onSave).onFailure { error = it.message ?: "Invalid profile" }
        }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
