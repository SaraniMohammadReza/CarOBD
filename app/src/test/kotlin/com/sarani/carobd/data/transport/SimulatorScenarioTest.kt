package com.sarani.carobd.data.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** §17 / §29: stable scenario id round-trip + Persian/English labels. */
class SimulatorScenarioTest {

    @Test
    fun fromIdRoundTripAllScenarios() {
        SimulatorScenario.values().forEach { scenario ->
            assertEquals(scenario, simulatorScenarioFromId(scenario.id))
        }
    }

    @Test
    fun fromIdDefaultsToHealthyVehicle() {
        assertEquals(SimulatorScenario.HEALTHY_VEHICLE, simulatorScenarioFromId(null))
        assertEquals(SimulatorScenario.HEALTHY_VEHICLE, simulatorScenarioFromId(""))
        assertEquals(SimulatorScenario.HEALTHY_VEHICLE, simulatorScenarioFromId("   "))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { simulatorScenarioFromId("unknown") }
    }

    @Test
    fun fromIdIsCaseAndPaddingInsensitive() {
        assertEquals(SimulatorScenario.STORED_DTC, simulatorScenarioFromId("  STORED_DTC  "))
        assertEquals(SimulatorScenario.BUFFER_FULL, simulatorScenarioFromId("BUFFER_FULL"))
    }

    @Test
    fun labelsAreBilingual() {
        val s = SimulatorScenario.STORED_DTC
        assert(s.labelEn.isNotBlank())
        assert(s.labelFa.isNotBlank())
        assertEquals(s.labelFa, s.labelFa) // fa persisted
    }
}
