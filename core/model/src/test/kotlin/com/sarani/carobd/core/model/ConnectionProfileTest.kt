package com.sarani.carobd.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §11 ConnectionProfile validation/sanization tests (§41: meaningful, not getters).
 */
class ConnectionProfileTest {

    @Test
    fun bluetoothClassicValid() {
        val p = profile(TransportConfig.BluetoothClassic("AA:BB:CC:DD:EE:FF"))
        assertTrue(p.validate().isEmpty())
    }

    @Test
    fun bluetoothClassicBlankAddressRejected() {
        val p = profile(TransportConfig.BluetoothClassic("  "))
        assertEquals(listOf("valid exact bluetooth address required"), p.validate())
    }

    @Test
    fun bleValidAndMtuNegativeRejected() {
        assertTrue(profile(TransportConfig.Ble("AA:BB:CC:DD:EE:FF")).validate().isEmpty())
        val p = profile(TransportConfig.Ble("AA:BB:CC:00:00:00", mtu = -1))
        assertEquals(listOf("Custom BLE UUID/MTU configuration is not implemented", "ble mtu must be positive"), p.validate())
    }

    @Test
    fun wifiValidPortRangeHostAndBadPort() {
        assertTrue(profile(TransportConfig.Wifi("192.168.0.10", 35000)).validate().isEmpty())
        assertEquals(listOf("explicit valid wifi host required"), profile(TransportConfig.Wifi(" ", 35000)).validate())
        assertEquals(listOf("wifi port must be in [1,65535]"), profile(TransportConfig.Wifi("h", 0)).validate())
        assertEquals(listOf("wifi port must be in [1,65535]"), profile(TransportConfig.Wifi("h", 70000)).validate())
    }

    @Test
    fun usbRequiresIdentity() {
        // deviceId alone is sufficient.
        assertTrue(profile(TransportConfig.Usb(deviceId = 7, portIndex = 0)).validate().isNullOrEmpty())
        // vendor+product is sufficient.
        assertTrue(profile(TransportConfig.Usb(vendorId = 0x1234, productId = 0x5678, portIndex = 0)).validate().isNullOrEmpty())
        // nothing identifying → rejected.
        val errs = profile(TransportConfig.Usb()).validate()
        assertEquals(listOf("usb device must have deviceId or vendorId+productId", "explicit usb interface required"), errs)
    }

    @Test
    fun simulatorAlwaysValid() {
        assertTrue(profile(TransportConfig.Simulator("healthy")).validate().isEmpty())
        assertTrue(profile(TransportConfig.Simulator()).validate().isEmpty())
    }

    @Test
    fun blankProfileIdAndNameRejectedAtConstruction() {
        var thrown = false
        try { ConnectionProfile(id = "  ", name = "x", transport = TransportConfig.Simulator()) }
        catch (e: IllegalArgumentException) { thrown = true }
        assertTrue(thrown)
        thrown = false
        try { ConnectionProfile(id = "x", name = "  ", transport = TransportConfig.Simulator()) }
        catch (e: IllegalArgumentException) { thrown = true }
        assertTrue(thrown)
    }

    @Test
    fun timeoutsClampedBySanitized() {
        val p = profile(TransportConfig.Wifi("h", 1), connectTimeoutMs = 5L, commandTimeoutMs = 99999L)
        val s = p.sanitized()
        assertEquals(ConnectionProfile.TIMEOUT_MIN, s.connectTimeoutMs)
        assertEquals(ConnectionProfile.TIMEOUT_MAX, s.commandTimeoutMs)
        // Within-bounds values are untouched.
        val ok = profile(TransportConfig.Wifi("h", 1), connectTimeoutMs = 3000L, commandTimeoutMs = 2500L)
        assertEquals(ok, ok.sanitized())
    }

    @Test
    fun timeoutsStillInvalidatedAfterSanitize() {
        // Sanitization only clamps numeric ranges; structural transport errors remain.
        val p = profile(TransportConfig.Wifi(" ", 35000), connectTimeoutMs = 5L)
        val s = p.sanitized()
        assertEquals(ConnectionProfile.TIMEOUT_MIN, s.connectTimeoutMs)
        assertEquals(listOf("explicit valid wifi host required"), s.validate())
    }

    @Test
    fun manualProtocolRequiresAndRestrictsCode() {
        val base = TransportConfig.Simulator()
        assertTrue(profile(base, mode = ProtocolMode.MANUAL, code = "6").validate().isEmpty())
        val missing = profile(base, mode = ProtocolMode.MANUAL, code = null)
        assertEquals(listOf("manualProtocolCode required for MANUAL protocol"), missing.validate())
        val unsupported = profile(base, mode = ProtocolMode.MANUAL, code = "Z")
        assertEquals(listOf("unsupported manual protocol code: Z"), unsupported.validate())
    }

    @Test
    fun newProfileGeneratesUniqueIds() {
        val a = ConnectionProfile.newProfile("BT", TransportConfig.BluetoothClassic("AA:BB"))
        val b = ConnectionProfile.newProfile("BT", TransportConfig.BluetoothClassic("CC:DD"))
        assertTrue(a.id.isNotBlank())
        assertTrue(b.id.isNotBlank())
        assertTrue(a.id != b.id)
    }

    private fun profile(
        transport: TransportConfig,
        mode: ProtocolMode = ProtocolMode.AUTO,
        code: String? = null,
        connectTimeoutMs: Long = 3000L,
        commandTimeoutMs: Long = 2500L,
    ) = ConnectionProfile(
        id = "test", name = "Test", transport = transport,
        protocolMode = mode, manualProtocolCode = code,
        connectTimeoutMs = connectTimeoutMs,
        commandTimeoutMs = commandTimeoutMs,
    )
}
