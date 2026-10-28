package com.sarani.carobd.core.adapter.profile

import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.ProtocolMode
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** §11 / §41 tests: JSON codec round-trip + repository selection/default/migration. */
class ConnectionProfileRepositoryTest {

    // ---- codec round-trip for every transport config ----
    @Test
    fun codecRoundTripAllTransportConfigs() = runTest {
        val profiles = listOf(
            ConnectionProfile.newProfile("BT Classic", TransportConfig.BluetoothClassic("AA:BB:CC:DD:EE:FF")),
            ConnectionProfile.newProfile("BLE", TransportConfig.Ble("AA:BB:00:00:00:11",
                serviceUuid = "00001101-0000-1000-8000-00805f9b34fb", mtu = 247)),
            ConnectionProfile.newProfile("WiFi", TransportConfig.Wifi("192.168.0.10", 35000)),
            ConnectionProfile.newProfile("USB", TransportConfig.Usb(vendorId = 0x1234, productId = 0x5678, deviceId = 9, portIndex = 0, baudRate = 9600)),
            ConnectionProfile.newProfile("Sim", TransportConfig.Simulator("healthy")),
            ConnectionProfile.newProfile("BLE auto", TransportConfig.Ble("AA:BB:CC:DD:EE:00")),
        ).map { it.copy(protocolMode = ProtocolMode.MANUAL, manualProtocolCode = "6") }

        for (p in profiles) {
            // encode must be valid JSON and decode must reproduce equality (id stable).
            val json = ConnectionProfileJson.encode(p)
            val back = ConnectionProfileJson.decode(json)
            assertEquals("round-trip failed for ${p.transport.type}", p, back)
            assertEquals(p.transport.type, back.transport.type)
        }
    }

    @Test
    fun codecHandlesAllProtocolModesAndNulls() = runTest {
        val p = ConnectionProfile.newProfile("wifi", TransportConfig.Wifi("192.168.1.50", 35001))
            .copy(
                protocolMode = ProtocolMode.VEHICLE_PROFILE,
                manualProtocolCode = null,
                vehicleProfileId = null,
                simulatorScenarioId = null,
                lastSuccessfulAt = null,
                autoReconnect = false,
                connectTimeoutMs = ConnectionProfile.TIMEOUT_MAX,
                commandTimeoutMs = ConnectionProfile.TIMEOUT_MIN,
            )
        val back = ConnectionProfileJson.decode(ConnectionProfileJson.encode(p))
        assertEquals(p, back)
        assertEquals(ProtocolMode.VEHICLE_PROFILE, back.protocolMode)
        assertNull(back.manualProtocolCode)
        assertNull(back.vehicleProfileId)
    }

    @Test
    fun codecUnknownTransportKindFallsBackToSimulator() {
        val json = """{"id":"x","name":"n","transport":{"kind":"BOGUS","deviceAddress":""},"protocolMode":"AUTO","manualProtocolCode":null,"vehicleProfileId":null,"autoReconnect":true,"connectTimeoutMs":3000,"commandTimeoutMs":2500,"simulatorScenarioId":null,"lastSuccessfulAt":null}"""
        val back = ConnectionProfileJson.decode(json)
        assertEquals(TransportType.SIMULATOR, back.transport.type)
    }

    // ---- repository ----
    private fun repo() = InMemoryConnectionProfileRepository()

    @Test
    fun roundTripCreateSelectDelete() = runTest {
        val r = repo()
        val id = r.saveAndSelect(ConnectionProfile.newProfile("BT", TransportConfig.BluetoothClassic("AA:BB:CC:DD:EE:FF")))!!
        assertTrue(id.isNotBlank())
        assertEquals(id, r.selectedProfileId.first())
        val sel = r.selectedProfile()!!
        assertEquals("BT", sel.name)
        assertEquals(TransportType.BLUETOOTH_CLASSIC, sel.transport.type)
        assertEquals(id, r.allProfiles.first().single().id)
        assertTrue(r.delete(id))
        assertEquals(0, r.allProfiles.first().size)
        assertNull(r.selectedProfileId.first())
    }

    @Test
    fun invalidProfileRejectedNotPersisted() = runTest {
        val r = repo()
        // blank name rejected at construction → save returns null and stores nothing.
        var rejected = false
        try {
            ConnectionProfile(id = "x", name = "  ", transport = TransportConfig.Simulator())
        } catch (e: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        // A structurally invalid transport (bad port) is rejected by save.
        val bad = ConnectionProfile(id = "y", name = "w", transport = TransportConfig.Wifi("h", 0))
        assertNull(r.save(bad))
        assertEquals(0, r.allProfiles.first().size)
    }

    @Test
    fun selectNonExistentFails() = runTest {
        val r = repo()
        assertFalse(r.select("nope"))
        assertNull(r.selectedProfileId.first())
    }

    @Test
    fun defaultProfileMigrationCreatesSimulatorDefault() = runTest {
        val r = repo()
        val d = r.defaultProfile()
        assertEquals(TransportType.SIMULATOR, d.transport.type)
        assertEquals(d.id, r.selectedProfileId.first())
        // second call returns existing (no duplicate).
        r.defaultProfile()
        assertEquals(1, r.allProfiles.first().size)
    }

    @Test
    fun updateReplacesAndPersists() = runTest {
        val r = repo()
        val id = r.saveAndSelect(ConnectionProfile.newProfile("BT", TransportConfig.BluetoothClassic("AA:BB:CC:DD:EE:FF")))!!
        // update: same id, new name + wifi config.
        val updated = r.selectedProfile()!!.copy(name = "WiFi replaced", transport = TransportConfig.Wifi("192.168.0.5", 35000))
        r.save(updated)
        val list = r.allProfiles.first()
        assertEquals(1, list.size)
        assertEquals("WiFi replaced", list.single().name)
        assertEquals(TransportType.WIFI_TCP, list.single().transport.type)
    }
}
