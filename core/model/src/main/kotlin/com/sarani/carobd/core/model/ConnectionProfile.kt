package com.sarani.carobd.core.model

import java.util.UUID

/**
 * §11 P1 REAL CONNECTIONPROFILE.
 *
 * A product-level, persisted connection profile. Transport configuration lives
 * in the [TransportConfig] sealed hierarchy so each transport carries only its
 * stable identifiers (no Android framework objects — §11 rule).
 *
 * Validation is explicit: [ConnectionProfile.validate] returns the list of
 * problems (empty when valid). Construction never silently accepts garbage.
 */
data class ConnectionProfile(
    val id: String,
    val name: String,
    val transport: TransportConfig,
    val protocolMode: ProtocolMode = ProtocolMode.AUTO,
    /** Raw ELM protocol code used only when [protocolMode] == MANUAL (e.g. "6" for CAN 11/500). */
    val manualProtocolCode: String? = null,
    val vehicleProfileId: String? = null,
    val autoReconnect: Boolean = true,
    val connectTimeoutMs: Long = 3000L,
    val commandTimeoutMs: Long = 2500L,
    val simulatorScenarioId: String? = null,
    val lastSuccessfulAt: Long? = null,
) {
    init {
        // Defensive invariants enforced at construction; [validate] is the
        // authoritative, exhaustive check used by the repository.
        require(id.isNotBlank()) { "profile id must not be blank" }
        require(name.isNotBlank()) { "profile name must not be blank" }
    }

    /** Exhaustive validation. Empty list == valid. Used by the repository to decide
     *  whether to persist or reject/sanitize a profile. */
    fun validate(): List<String> {
        val errs = mutableListOf<String>()
        if (id.isBlank()) errs.add("id must not be blank")
        if (name.isBlank()) errs.add("name must not be blank")
        errs.addAll(transport.validate())
        when (protocolMode) {
            ProtocolMode.MANUAL -> {
                if (manualProtocolCode.isNullOrBlank()) errs.add("manualProtocolCode required for MANUAL protocol")
                else if (!isAllowedManualCode(manualProtocolCode)) errs.add("unsupported manual protocol code: $manualProtocolCode")
            }
            ProtocolMode.AUTO, ProtocolMode.VEHICLE_PROFILE -> Unit
        }
        if (connectTimeoutMs !in TIMEOUT_MIN..TIMEOUT_MAX) errs.add("connectTimeoutMs out of range [$TIMEOUT_MIN,$TIMEOUT_MAX]")
        if (commandTimeoutMs !in TIMEOUT_MIN..TIMEOUT_MAX) errs.add("commandTimeoutMs out of range [$TIMEOUT_MIN,$TIMEOUT_MAX]")
        return errs
    }

    /** Sanitizing copy: clamps the timeout window; leaves structural validation
     *  (addresses/ports/etc.) to [validate], which rejects. Returns a profile
     *  whose numeric fields are within bounds. */
    fun sanitized(): ConnectionProfile = copy(
        connectTimeoutMs = connectTimeoutMs.coerceIn(TIMEOUT_MIN, TIMEOUT_MAX),
        commandTimeoutMs = commandTimeoutMs.coerceIn(TIMEOUT_MIN, TIMEOUT_MAX),
    )

    companion object {
        const val TIMEOUT_MIN = 100L
        const val TIMEOUT_MAX = 30000L

        /** New ephemeral profile (not yet persisted; repository assigns id on save). */
        fun newProfile(name: String, transport: TransportConfig): ConnectionProfile =
            ConnectionProfile(id = UUID.randomUUID().toString(), name = name, transport = transport)

        /** ELM protocol codes the manual UI may present (§18 subset). */
        private val ALLOWED_MANUAL_CODES = setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A")
        private fun isAllowedManualCode(code: String): Boolean = code.trim().uppercase() in ALLOWED_MANUAL_CODES
    }
}

/**
 * §11 Transport configuration. Stable identifiers only — no Android
 * [android.bluetooth.BluetoothDevice], [android.hardware.usb.UsbDevice], etc.
 * The transport factory (app layer) consumes these strings to build an
 * [com.sarani.carobd.core.adapter.transport.ObdTransport].
 */
sealed interface TransportConfig {
    val type: TransportType
    /** Human-readable, stable target identifier for display (address, host:port, device, scenario). */
    val connectionTarget: String?
        get() = when (this) {
            is BluetoothClassic -> deviceAddress
            is Ble -> deviceAddress ?: serviceUuid
            is Wifi -> "$host:$port"
            is Usb -> deviceId?.toString()
                ?: (if (vendorId != null && productId != null) "VID $vendorId/PID $productId" else null)
            is Simulator -> scenarioId ?: transcriptId ?: "simulator"
        }

    data class BluetoothClassic(val deviceAddress: String) : TransportConfig {
        override val type = TransportType.BLUETOOTH_CLASSIC
        override fun validate() = if (!deviceAddress.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))) listOf("valid exact bluetooth address required") else emptyList()
    }

    data class Ble(
        val deviceAddress: String,
        val serviceUuid: String? = null,
        val txUuid: String? = null,
        val rxUuid: String? = null,
        /** MTU in bytes; null = adapter default. Must be > 0 if present. */
        val mtu: Int? = null,
    ) : TransportConfig {
        override val type = TransportType.BLUETOOTH_LE
        override fun validate(): List<String> {
            val e = mutableListOf<String>()
            if (!deviceAddress.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))) e.add("valid exact ble address required")
            if (serviceUuid != null || txUuid != null || rxUuid != null || mtu != null) e.add("Custom BLE UUID/MTU configuration is not implemented")
            if (mtu != null && mtu <= 0) e.add("ble mtu must be positive")
            return e
        }
    }

    data class Wifi(val host: String, val port: Int) : TransportConfig {
        override val type = TransportType.WIFI_TCP
        override fun validate(): List<String> {
            val e = mutableListOf<String>()
            if (host.isBlank() || host != host.trim() || host.any { it.isWhitespace() || it.code < 32 } || host.contains('/')) e.add("explicit valid wifi host required")
            if (port !in PORT_MIN..PORT_MAX) e.add("wifi port must be in [$PORT_MIN,$PORT_MAX]")
            return e
        }
    }

    data class Usb(
        val vendorId: Int? = null,
        val productId: Int? = null,
        val deviceId: Int? = null,
        val portIndex: Int? = null,
        val baudRate: Int? = null,
    ) : TransportConfig {
        override val type = TransportType.USB_SERIAL
        override fun validate(): List<String> {
            val e = mutableListOf<String>()
            val hasIdentity = deviceId != null || (vendorId != null && productId != null)
            if (!hasIdentity) e.add("usb device must have deviceId or vendorId+productId")
            if (portIndex == null) e.add("explicit usb interface required")
            if (vendorId != null && vendorId !in 0..65535) e.add("invalid USB VID")
            if (productId != null && productId !in 0..65535) e.add("invalid USB PID")
            if (portIndex != null && portIndex < 0) e.add("usb portIndex must be >= 0")
            if (baudRate != null && baudRate <= 0) e.add("usb baudRate must be positive")
            return e
        }
    }

    data class Simulator(val scenarioId: String? = null, val transcriptId: String? = null) : TransportConfig {
        override val type = TransportType.SIMULATOR
        override fun validate(): List<String> = emptyList()
    }

    fun validate(): List<String> = when (this) {
        is BluetoothClassic -> validate()
        is Ble -> validate()
        is Wifi -> validate()
        is Usb -> validate()
        is Simulator -> validate()
    }

    companion object {
        const val PORT_MIN = 1
        const val PORT_MAX = 65535

        /** Convenience: the well-known Wi-Fi ELM default is only a *template*,
         *  never a universal default (§15). */
        val WIFI_TEMPLATE = Wifi("192.168.0.10", 35000)
        fun wifi(host: String, port: Int) = Wifi(host, port)
    }
}

/**
 * §18 Protocol-mode selector.
 * - AUTO: ELM auto-protocol (ATSP 0).
 * - VEHICLE_PROFILE: use the selected DiagnosticProfile's recommendation.
 * - MANUAL: explicit forced protocol via [ConnectionProfile.manualProtocolCode].
 */
enum class ProtocolMode { AUTO, VEHICLE_PROFILE, MANUAL }
