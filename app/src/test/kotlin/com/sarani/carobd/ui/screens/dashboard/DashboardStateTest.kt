package com.sarani.carobd.ui.screens.dashboard

import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.protocol.iranian.IranianEcuRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardStateTest {

    private val dena = IranianEcuRegistry.PRESET_VEHICLES.first()

    @Test
    fun nullVehicle_labelIsSelectPrompt() {
        val state = DashboardState()
        assertEquals("خودروی تشخیصی انتخاب نشده", state.vehicleDisplayLabel())
        assertFalse(state.hasActiveVehicle)
    }

    @Test
    fun selectedVehicle_labelIsProfileModelName() {
        val state = DashboardState(activeProfile = dena)
        assertEquals(dena.modelNameFa, state.vehicleDisplayLabel())
        assertTrue(state.hasActiveVehicle)
    }

    @Test
    fun simulatorIndicatorRequiresActualConnectedSession() {
        assertFalse(DashboardState(selectedConnectionType = ConnectionType.VIRTUAL_SIMULATOR).isSimulator)
        val sim = com.sarani.carobd.core.model.ConnectionState.Connected("sim", "ELM", "CAN", "test", 12f, isSimulator = true)
        assertTrue(DashboardState(connectionState = sim, selectedConnectionType = ConnectionType.WIFI_TCP).isSimulator)
        assertFalse(DashboardState(connectionState = sim.copy(isSimulator = false), selectedConnectionType = ConnectionType.VIRTUAL_SIMULATOR).isSimulator)
    }

    @Test
    fun primaryMetricPids_areSpeedRpmCoolantBatteryInOrder() {
        // §4: first-screen primary metrics must be exactly Speed/RPM/Coolant/
        // Battery in that order — and must NOT also render as 132dp circular
        // gauges (PrimaryMetricsRow owns them; defaults no longer include them).
        assertEquals(listOf("010D", "010C", "0105", "ATRV"), primaryMetricPids())
    }

    @Test
    fun defaultTelemetryMarksPrimaryPidsUnmeasured() {
        // §4 / §8 truthfulness: with no live telemetry, every primary PID is
        // "not measured" → PrimaryMetricsRow renders "—", never a fabricated 0.
        val empty = DashboardState().telemetry
        assertFalse(empty.isMeasured("010D")) // speed
        assertFalse(empty.isMeasured("010C")) // rpm
        assertFalse(empty.isMeasured("0105")) // coolant
        assertFalse(empty.isMeasured("0142")) // battery
    }
}
