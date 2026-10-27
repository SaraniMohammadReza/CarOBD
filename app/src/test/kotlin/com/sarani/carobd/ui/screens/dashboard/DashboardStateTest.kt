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
    fun isSimulator_trueOnlyForVirtualSimulator() {
        assertTrue(
            DashboardState(selectedConnectionType = ConnectionType.VIRTUAL_SIMULATOR).isSimulator
        )
        assertFalse(
            DashboardState(selectedConnectionType = ConnectionType.BLUETOOTH_CLASSIC).isSimulator
        )
        assertFalse(
            DashboardState(selectedConnectionType = ConnectionType.WIFI_TCP).isSimulator
        )
    }
}
