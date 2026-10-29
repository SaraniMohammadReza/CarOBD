package com.sarani.carobd.core.model
import org.junit.Assert.*
import org.junit.Test
class UsbTargetSelectionTest {
    private val devices = listOf(UsbDeviceIdentity(1, 10, 20, 2), UsbDeviceIdentity(2, 30, 40, 1))
    @Test fun usb_missingRequestedTargetNeverFallsBack() {
        try { UsbTargetSelection.select(TransportConfig.Usb(deviceId = 3, portIndex = 0), devices); fail("must fail") }
        catch (expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("DEVICE_UNAVAILABLE")) }
    }
    @Test fun usb_identityAndPortMustMatch() {
        assertEquals(2, UsbTargetSelection.select(TransportConfig.Usb(vendorId = 30, productId = 40, portIndex = 0), devices).deviceId)
        for (config in listOf(TransportConfig.Usb(), TransportConfig.Usb(deviceId = 1), TransportConfig.Usb(deviceId = 1, vendorId = 30, portIndex = 0), TransportConfig.Usb(deviceId = 2, portIndex = 1))) {
            try { UsbTargetSelection.select(config, devices); fail("must fail: $config") } catch (_: IllegalArgumentException) {}
        }
    }
    @Test fun usb_ambiguousVidPidFailsClosed() {
        try { UsbTargetSelection.select(TransportConfig.Usb(vendorId = 10, productId = 20, portIndex = 0), devices + devices[0].copy(deviceId = 3)); fail("must fail") }
        catch (_: IllegalArgumentException) {}
    }
}
