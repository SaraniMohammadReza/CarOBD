package com.sarani.carobd.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sarani.carobd.core.connectivity.*
import kotlinx.coroutines.*

@Composable
fun BluetoothTargetPicker(ble: Boolean, onSelected: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var devices by remember(ble) { mutableStateOf<List<DiscoveredBluetoothDevice>>(emptyList()) }
    var error by remember(ble) { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, ble) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) job?.cancel() }
        owner.lifecycle.addObserver(observer)
        onDispose { job?.cancel(); owner.lifecycle.removeObserver(observer) }
    }
    Column {
        RuntimeAccessControls()
        TextButton(onClick = {
            job?.cancel()
            devices = emptyList()
            error = null
            job = scope.launch {
                try {
                    withTimeoutOrNull(10_000) {
                        val results = if (ble) BleDeviceDiscovery(context).scan() else BluetoothDeviceScanner(context).scanNearbyDevices()
                        results.collect { devices = it }
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "Bluetooth discovery failed" }
            }
        }) { Text(if (ble) "Scan BLE (10 s)" else "Scan Classic (10 s)") }
        error?.let { Text(it) }
        devices.forEach { device -> TextButton(onClick = { job?.cancel(); onSelected(device.address) }) { Text("${device.name} · ${device.address}") } }
    }
}
