package com.sarani.carobd.core.model

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data class PermissionRequired(val permissions: List<String>) : ConnectionState
    data object TransportScanning : ConnectionState
    data class DeviceSelected(val deviceName: String, val address: String) : ConnectionState
    data class TransportConnecting(val target: String) : ConnectionState
    data class TransportConnected(val target: String) : ConnectionState
    data object AdapterResetting : ConnectionState
    data class AdapterIdentifying(val probeStage: String) : ConnectionState
    data class AdapterConfiguring(val setting: String) : ConnectionState
    data class ProtocolDetecting(val candidateName: String, val attemptIndex: Int, val totalCandidates: Int) : ConnectionState
    data class EcuDiscovering(val protocolName: String) : ConnectionState
    data class CapabilityDiscovering(val supportedPidCount: Int) : ConnectionState
    data class Connected(
        val deviceName: String,
        val adapterType: String,
        val protocolName: String,
        val elmVersion: String,
        val batteryVoltage: Float,
        val responderCount: Int = 1,
        val isSimulator: Boolean = false
    ) : ConnectionState
    data class Degraded(val reason: String, val consecutiveErrorCount: Int) : ConnectionState
    data class Retrying(val attempt: Int, val maxAttempts: Int, val lastError: String) : ConnectionState
    data object Disconnecting : ConnectionState
    data object Disconnected : ConnectionState
    data class Failure(
        val stage: String,
        val humanMessageFa: String,
        val technicalMessage: String,
        val isRecoverable: Boolean,
        val rawResponse: String? = null
    ) : ConnectionState
}

enum class TransportType {
    BLUETOOTH_CLASSIC,
    BLUETOOTH_LE,
    WIFI_TCP,
    USB_SERIAL,
    SIMULATOR
}

data class TransportError(
    val code: String,
    val message: String,
    val cause: Throwable? = null
)
