package com.sarani.carobd.core.connectivity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.sarani.carobd.core.model.RuntimeAccess
import com.sarani.carobd.core.permissions.AndroidAccess
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/** One lifecycle-owned scan; emitted addresses are explicit selection targets, never fallbacks. */
class BleDeviceDiscovery(private val context: Context) {
    @SuppressLint("MissingPermission")
    fun scan() = callbackFlow<List<DiscoveredBluetoothDevice>> {
        AndroidAccess.require(context, RuntimeAccess.Scan)
        val scanner = context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
            ?: error("BLE scanning unavailable")
        val found = linkedMapOf<String, DiscoveredBluetoothDevice>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    val address = result.device.address
                    val name = result.scanRecord?.deviceName ?: result.device.name ?: "—"
                    val snapshot = synchronized(found) {
                        found[address] = DiscoveredBluetoothDevice(name, address, false, false)
                        found.values.toList()
                    }
                    trySend(snapshot)
                } catch (e: SecurityException) { close(e) }
            }
            override fun onScanFailed(errorCode: Int) { close(IllegalStateException("BLE scan failed ($errorCode)")) }
        }
        try { scanner.startScan(callback) } catch (e: SecurityException) { close(e); return@callbackFlow }
        awaitClose { runCatching { scanner.stopScan(callback) } }
    }
}
