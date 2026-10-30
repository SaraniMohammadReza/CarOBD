package com.sarani.carobd.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sarani.carobd.core.model.ConnectionIssue
import com.sarani.carobd.R

@Composable fun connectionIssueLabel(issue: ConnectionIssue?): String = stringResource(when (issue) {
    ConnectionIssue.PermissionDenied -> R.string.issue_permission
    ConnectionIssue.PermanentlyDenied -> R.string.issue_permanent
    ConnectionIssue.BluetoothDisabled -> R.string.issue_bluetooth_off
    ConnectionIssue.BluetoothUnavailable, ConnectionIssue.UnsupportedDevice -> R.string.issue_unsupported
    ConnectionIssue.BleScanFailed -> R.string.issue_ble_scan
    ConnectionIssue.BleSubscriptionFailed -> R.string.issue_ble_subscription
    ConnectionIssue.UsbPermissionDenied -> R.string.issue_usb_permission
    ConnectionIssue.UsbTargetMissing -> R.string.issue_usb_target
    ConnectionIssue.WifiTimeout -> R.string.issue_wifi_timeout
    ConnectionIssue.TransportEof -> R.string.issue_transport_eof
    ConnectionIssue.AdapterTimeout, ConnectionIssue.ConnectTimeout -> R.string.issue_timeout
    ConnectionIssue.ProtocolMismatch -> R.string.issue_protocol
    ConnectionIssue.ForegroundServiceUnavailable -> R.string.issue_service
    else -> R.string.issue_unknown
})
