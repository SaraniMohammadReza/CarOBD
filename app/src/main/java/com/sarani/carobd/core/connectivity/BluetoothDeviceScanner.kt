package com.sarani.carobd.core.connectivity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val isObdCandidate: Boolean
)

class BluetoothDeviceScanner(private val context: Context) {

    private val obdKeywords = listOf("OBD", "ELM", "VIECAR", "VGATE", "VLINKER", "ICAR", "CAR", "BT", "DIAG", "SCAN")

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<DiscoveredBluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        val bonded = adapter.bondedDevices ?: return emptyList()

        return bonded.map { dev ->
            val name = dev.name ?: "Unknown Device"
            DiscoveredBluetoothDevice(
                name = name,
                address = dev.address,
                isBonded = true,
                isObdCandidate = isLikelyObd(name)
            )
        }.sortedByDescending { it.isObdCandidate }
    }

    @SuppressLint("MissingPermission")
    fun scanNearbyDevices(): Flow<List<DiscoveredBluetoothDevice>> = callbackFlow {
        val foundMap = mutableMapOf<String, DiscoveredBluetoothDevice>()

        // Preload bonded devices
        getPairedDevices().forEach { foundMap[it.address] = it }
        trySend(foundMap.values.toList())

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null) {
                            val name = device.name ?: "Unknown Bluetooth Device"
                            val address = device.address
                            val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
                            foundMap[address] = DiscoveredBluetoothDevice(
                                name = name,
                                address = address,
                                isBonded = isBonded,
                                isObdCandidate = isLikelyObd(name)
                            )
                            trySend(foundMap.values.toList().sortedByDescending { it.isObdCandidate })
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)

        adapter.startDiscovery()

        awaitClose {
            try {
                adapter.cancelDiscovery()
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    private fun isLikelyObd(name: String): Boolean {
        val upper = name.uppercase()
        return obdKeywords.any { upper.contains(it) }
    }
}
