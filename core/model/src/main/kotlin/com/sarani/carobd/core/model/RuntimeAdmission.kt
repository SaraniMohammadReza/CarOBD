package com.sarani.carobd.core.model

enum class AccessState { Granted, NotRequested, Denied, PermanentlyDenied, UnsupportedDevice, BluetoothUnavailable, BluetoothDisabled, LocationDisabled }
enum class RuntimeAccess { Connect, Scan, Notifications, LocalNetwork }
data class AccessSnapshot(
    val supported: Boolean = true,
    val permissionGranted: Boolean,
    val requested: Boolean = false,
    val showRationale: Boolean = false,
    val bluetoothAvailable: Boolean = true,
    val bluetoothEnabled: Boolean = true,
)
object RuntimeAdmission {
    fun evaluate(s: AccessSnapshot): AccessState = when {
        !s.supported -> AccessState.UnsupportedDevice
        !s.permissionGranted && !s.requested -> AccessState.NotRequested
        !s.permissionGranted && s.showRationale -> AccessState.Denied
        !s.permissionGranted -> AccessState.PermanentlyDenied
        !s.bluetoothAvailable -> AccessState.BluetoothUnavailable
        !s.bluetoothEnabled -> AccessState.BluetoothDisabled
        else -> AccessState.Granted
    }
}
class AccessDeniedException(val access: RuntimeAccess, val state: AccessState) : IllegalStateException("$access: $state")
