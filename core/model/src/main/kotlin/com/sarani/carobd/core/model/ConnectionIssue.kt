package com.sarani.carobd.core.model

enum class ConnectionIssue {
    PermissionDenied, PermanentlyDenied, BluetoothDisabled, BluetoothUnavailable, UnsupportedDevice,
    BleScanFailed, BleSubscriptionFailed, UsbPermissionDenied, UsbTargetMissing, WifiTimeout,
    TransportEof, ConnectTimeout, AdapterTimeout, ProtocolMismatch, ForegroundServiceUnavailable, Unknown,
}
class ConnectionIssueException(val issue: ConnectionIssue, message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
fun connectionIssue(error: Throwable): ConnectionIssue = when (error) {
    is ConnectionIssueException -> error.issue
    is AccessDeniedException -> when (error.state) {
        AccessState.PermanentlyDenied -> ConnectionIssue.PermanentlyDenied
        AccessState.BluetoothDisabled -> ConnectionIssue.BluetoothDisabled
        AccessState.BluetoothUnavailable -> ConnectionIssue.BluetoothUnavailable
        AccessState.UnsupportedDevice -> ConnectionIssue.UnsupportedDevice
        else -> ConnectionIssue.PermissionDenied
    }
    is java.net.SocketTimeoutException -> ConnectionIssue.WifiTimeout
    is SecurityException -> ConnectionIssue.PermissionDenied
    else -> error.cause?.takeIf { it !== error }?.let(::connectionIssue) ?: ConnectionIssue.Unknown
}
