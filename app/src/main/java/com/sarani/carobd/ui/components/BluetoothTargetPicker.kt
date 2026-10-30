package com.sarani.carobd.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sarani.carobd.R
import com.sarani.carobd.core.connectivity.*
import com.sarani.carobd.core.model.RuntimeAccess
import kotlinx.coroutines.*

/** Discovery never selects or connects a target. The owner must use the exact selected address. */
@Composable
fun BluetoothTargetPicker(ble: Boolean, onSelected: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var devices by remember(ble) { mutableStateOf<List<DiscoveredBluetoothDevice>>(emptyList()) }
    var error by remember(ble) { mutableStateOf<String?>(null) }
    var scanning by remember(ble) { mutableStateOf(false) }
    var attempted by remember(ble) { mutableStateOf(false) }
    var timedOut by remember(ble) { mutableStateOf(false) }
    var canceled by remember(ble) { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, ble) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) job?.cancel() }
        owner.lifecycle.addObserver(observer)
        onDispose { job?.cancel(); owner.lifecycle.removeObserver(observer) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(if (ble) R.string.qa_ble_help else R.string.qa_classic_help))
        RuntimeAccessControls(setOf(RuntimeAccess.Scan))
        Column {
            TextButton(enabled = !scanning, onClick = {
                val previousScan = job
                previousScan?.cancel()
                job = scope.launch {
                    previousScan?.join()
                    devices = emptyList()
                    error = null
                    attempted = true
                    timedOut = false
                    canceled = false
                    scanning = true
                    try {
                        val completed = withTimeoutOrNull(10_000) {
                            val results = if (ble) BleDeviceDiscovery(context).scan() else BluetoothDeviceScanner(context).scanNearbyDevices()
                            results.collect { devices = it }
                            true
                        }
                        timedOut = completed == null
                    } catch (e: CancellationException) { canceled = true; throw e }
                    catch (e: Exception) { error = e.message ?: e.javaClass.simpleName }
                    finally { scanning = false }
                }
            }) { Text(stringResource(if (ble) R.string.ui_scan_ble else R.string.ui_scan_classic)) }
            if (scanning) TextButton(onClick = { job?.cancel() }) { Text(stringResource(R.string.qa_stop_scan)) }
        }
        when {
            scanning -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.qa_scanning)) }
            error != null -> { Text(stringResource(R.string.qa_scan_failed), color = MaterialTheme.colorScheme.error); DeveloperEvidence(error!!) }
            canceled -> Text(stringResource(R.string.qa_scan_canceled))
            timedOut && devices.isEmpty() -> Text(stringResource(R.string.qa_scan_timeout))
            attempted && devices.isEmpty() -> Text(stringResource(R.string.qa_scan_empty))
            !attempted -> Text(stringResource(R.string.qa_scan_idle))
            else -> Text(pluralStringResource(R.plurals.qa_scan_found, devices.size, devices.size))
        }
        devices.forEach { device ->
            OutlinedButton(onClick = { job?.cancel(); onSelected(device.address) }, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(device.name)
                    DiagnosticIdentifier(device.address)
                    Text(stringResource(if (device.isBonded) R.string.qa_paired else R.string.qa_discovered))
                }
            }
        }
    }
}
