package com.sarani.carobd.data.transport

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.TransportConfig

/**
 * §11 / §14 / §15 / §16 / §17 Factory: builds a concrete [ObdTransport] from a
 * persisted [TransportConfig]. The transport class is chosen *only* by
 * [TransportConfig.type] — there is no BLE→Classic misrouting (§14).
 */
object TransportFactory {

    /**
     * @return a transport for [config], or fails fast (with a clear message) when
     *   required stable identifiers are missing. The returned transport's
     *   [ObdTransport.transportType] always equals [config.type].
     */
    fun createTransport(
        config: TransportConfig,
        context: Context,
        bluetoothAdapter: BluetoothAdapter? =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter,
    ): ObdTransport {
        com.sarani.carobd.core.permissions.AndroidAccess.requireTransport(context, config)
        require(config.validate().isEmpty()) { config.validate().joinToString("; ") }
        val transport = when (config) {
        is TransportConfig.BluetoothClassic -> {
            if (config.deviceAddress.isBlank()) error("bluetooth address required")
            BluetoothClassicTransport(config.deviceAddress, bluetoothAdapter)
        }
        is TransportConfig.Ble -> {
            if (config.deviceAddress.isBlank()) error("ble address required")
            BleTransport(context, config.deviceAddress, bluetoothAdapter)
        }
        is TransportConfig.Wifi -> {
            if (config.host.isBlank()) error("wifi host required")
            WifiTcpTransport(host = config.host, port = config.port)
        }
        is TransportConfig.Usb -> UsbSerialTransport(
            context,
            selection = config,
            baudRate = config.baudRate ?: 38400,
        )
        is TransportConfig.Simulator -> {
            // §17: honor persisted scenarioId; null/blank → HEALTHY_VEHICLE (documented).
            IsolatedSimulatorTransport(simulatorScenarioFromId(config.scenarioId))
        }
    }
        // Runtime grants can be revoked after initial admission. All physical
        // BT/BLE/LAN writes and the session's passive link monitor re-use the
        // same AndroidAccess policy; simulator and USB keep their own lifecycles.
        return if (config is TransportConfig.BluetoothClassic || config is TransportConfig.Ble || config is TransportConfig.Wifi)
            com.sarani.carobd.core.adapter.transport.AdmissionGuardedTransport(transport) {
                com.sarani.carobd.core.permissions.AndroidAccess.requireTransport(context, config)
            } else transport
    }
}
