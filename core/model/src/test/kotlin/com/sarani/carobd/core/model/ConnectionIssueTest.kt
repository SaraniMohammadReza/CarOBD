package com.sarani.carobd.core.model

import org.junit.Assert.*
import org.junit.Test

class ConnectionIssueTest {
    @Test fun permissionAndPlatformFailuresStayTyped() {
        assertEquals(ConnectionIssue.PermanentlyDenied, connectionIssue(AccessDeniedException(RuntimeAccess.Connect, AccessState.PermanentlyDenied)))
        assertEquals(ConnectionIssue.BluetoothDisabled, connectionIssue(AccessDeniedException(RuntimeAccess.Connect, AccessState.BluetoothDisabled)))
        assertEquals(ConnectionIssue.WifiTimeout, connectionIssue(IllegalStateException("wrapper", java.net.SocketTimeoutException())))
        assertEquals(ConnectionIssue.Unknown, connectionIssue(IllegalStateException("unknown")))
    }
    @Test fun absentUsbTargetHasActionableTypeWithoutFallback() {
        val error = assertThrows(ConnectionIssueException::class.java) {
            UsbTargetSelection.select(TransportConfig.Usb(deviceId = 7, portIndex = 0), listOf(UsbDeviceIdentity(8, 1, 1, 1)))
        }
        assertEquals(ConnectionIssue.UsbTargetMissing, error.issue)
    }
}
