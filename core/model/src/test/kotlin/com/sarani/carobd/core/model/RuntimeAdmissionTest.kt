package com.sarani.carobd.core.model

import org.junit.Assert.*
import org.junit.Test

class RuntimeAdmissionTest {
    @Test fun bluetoothPermission_missingReturnsTypedState() {
        assertEquals(AccessState.NotRequested, RuntimeAdmission.evaluate(AccessSnapshot(permissionGranted = false)))
    }
    @Test fun bluetoothPermission_permanentlyDeniedActionable() {
        assertEquals(AccessState.PermanentlyDenied, RuntimeAdmission.evaluate(AccessSnapshot(permissionGranted = false, requested = true)))
        assertEquals(AccessState.Denied, RuntimeAdmission.evaluate(AccessSnapshot(permissionGranted = false, requested = true, showRationale = true)))
    }
    @Test fun bluetoothDisabledDistinctFromPermissionDenied() {
        assertEquals(AccessState.BluetoothDisabled, RuntimeAdmission.evaluate(AccessSnapshot(permissionGranted = true, bluetoothEnabled = false)))
        assertEquals(AccessState.NotRequested, RuntimeAdmission.evaluate(AccessSnapshot(permissionGranted = false, bluetoothEnabled = false)))
        assertEquals(AccessState.BluetoothUnavailable, RuntimeAdmission.evaluate(AccessSnapshot(permissionGranted = true, bluetoothAvailable = false)))
    }
    @Test fun unsupportedIsDistinct() {
        assertEquals(AccessState.UnsupportedDevice, RuntimeAdmission.evaluate(AccessSnapshot(supported = false, permissionGranted = true)))
    }
}
