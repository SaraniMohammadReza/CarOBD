package com.sarani.carobd.core.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Initializing(val step: String) : ConnectionState
    data class Connected(
        val deviceName: String,
        val protocolName: String,
        val elmVersion: String
    ) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

enum class ConnectionType {
    VIRTUAL_SIMULATOR,
    BLUETOOTH_CLASSIC,
    BLUETOOTH_LE,
    WIFI_TCP
}
