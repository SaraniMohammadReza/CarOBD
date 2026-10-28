package com.sarani.carobd.core.adapter.profile

import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.ProtocolMode
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.core.model.TransportType

/**
 * §11 JSON codec for [ConnectionProfile] ↔ string, used by the DataStore-backed
 * repository. Round-trips every transport config + protocol mode + settings.
 * Unknown fields are ignored on decode (forward compatibility), unknown
 * TransportConfig subclasses fall back to [TransportConfig.Simulator] so a
 * corrupt/peer profile can never crash the store.
 */
object ConnectionProfileJson {

    fun encode(profile: ConnectionProfile): String = Json.encode(mapOf(
        "id" to profile.id,
        "name" to profile.name,
        "protocolMode" to profile.protocolMode.name,
        "manualProtocolCode" to profile.manualProtocolCode,
        "vehicleProfileId" to profile.vehicleProfileId,
        "autoReconnect" to profile.autoReconnect,
        "connectTimeoutMs" to profile.connectTimeoutMs,
        "commandTimeoutMs" to profile.commandTimeoutMs,
        "simulatorScenarioId" to profile.simulatorScenarioId,
        "lastSuccessfulAt" to profile.lastSuccessfulAt,
        "transport" to encodeTransport(profile.transport),
    ))

    fun decode(json: String): ConnectionProfile {
        val m = Json.decode(json) as? Map<*, *> ?: error("profile json must be an object")
        @Suppress("UNCHECKED_CAST")
        fun s(key: String) = (m[key] as? String?) ?: ""
        fun l(key: String): Long = (m[key] as? Number)?.toLong() ?: 0L
        fun b(key: String): Boolean = m[key] == true
        fun nullableString(key: String): String? =
            (m[key] as? String?)?.takeIf { it.isNotEmpty() && it != "null" }

        val t = decodeTransport(m["transport"] as? Map<*,*> ?: emptyMap<String, Any?>())
        return ConnectionProfile(
            id = s("id").ifEmpty { error("profile id missing") },
            name = s("name").ifEmpty { error("profile name missing") },
            transport = t,
            protocolMode = nullableString("protocolMode")?.let { runCatching { ProtocolMode.valueOf(it) }.getOrNull() } ?: ProtocolMode.AUTO,
            manualProtocolCode = nullableString("manualProtocolCode"),
            vehicleProfileId = nullableString("vehicleProfileId"),
            autoReconnect = b("autoReconnect"),
            connectTimeoutMs = l("connectTimeoutMs"),
            commandTimeoutMs = l("commandTimeoutMs"),
            simulatorScenarioId = nullableString("simulatorScenarioId"),
            lastSuccessfulAt = (m["lastSuccessfulAt"] as? Number)?.toLong(),
        )
    }

    private fun encodeTransport(t: TransportConfig): Map<String, Any?> = when (t) {
        is TransportConfig.BluetoothClassic -> mapOf("kind" to "BLUETOOTH_CLASSIC", "deviceAddress" to t.deviceAddress)
        is TransportConfig.Ble -> mapOf(
            "kind" to "BLUETOOTH_LE", "deviceAddress" to t.deviceAddress,
            "serviceUuid" to t.serviceUuid, "txUuid" to t.txUuid, "rxUuid" to t.rxUuid, "mtu" to t.mtu,
        )
        is TransportConfig.Wifi -> mapOf("kind" to "WIFI_TCP", "host" to t.host, "port" to t.port)
        is TransportConfig.Usb -> mapOf(
            "kind" to "USB_SERIAL", "vendorId" to t.vendorId, "productId" to t.productId,
            "deviceId" to t.deviceId, "portIndex" to t.portIndex, "baudRate" to t.baudRate,
        )
        is TransportConfig.Simulator -> mapOf(
            "kind" to "SIMULATOR", "scenarioId" to t.scenarioId, "transcriptId" to t.transcriptId,
        )
    }

    private fun decodeTransport(m: Map<*, *>): TransportConfig {
        @Suppress("UNCHECKED_CAST")
        fun s(key: String) = (m[key] as? String?) ?: ""
        fun i(key: String): Int? = (m[key] as? Number)?.toInt()
        fun nullableString(key: String): String? =
            (m[key] as? String?)?.takeIf { it.isNotEmpty() && it != "null" }
        return when (s("kind")) {
            "BLUETOOTH_CLASSIC" -> TransportConfig.BluetoothClassic(s("deviceAddress"))
            "BLUETOOTH_LE" -> TransportConfig.Ble(
                deviceAddress = s("deviceAddress"),
                serviceUuid = nullableString("serviceUuid"),
                txUuid = nullableString("txUuid"),
                rxUuid = nullableString("rxUuid"),
                mtu = i("mtu"),
            )
            "WIFI_TCP" -> TransportConfig.Wifi(s("host"), i("port") ?: 0)
            "USB_SERIAL" -> TransportConfig.Usb(
                vendorId = i("vendorId"), productId = i("productId"),
                deviceId = i("deviceId"), portIndex = i("portIndex"), baudRate = i("baudRate"),
            )
            "SIMULATOR" -> TransportConfig.Simulator(nullableString("scenarioId"), nullableString("transcriptId"))
            else -> TransportConfig.Simulator() // unknown kind → safe fallback
        }
    }
}
