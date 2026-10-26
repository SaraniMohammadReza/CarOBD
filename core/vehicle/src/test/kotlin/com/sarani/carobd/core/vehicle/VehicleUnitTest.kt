package com.sarani.carobd.core.vehicle

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.vehicle.specs.VehicleSpecDatabase
import org.junit.Assert.*
import org.junit.Test

class VehicleUnitTest {

    @Test
    fun testVehicleSpecDatabase() {
        val tu5Specs = VehicleSpecDatabase.getSpecSheet("ikco_206_tu5")
        assertNotNull(tu5Specs)
        assertEquals(3.75f, tu5Specs!!.engineOilCapacityLiters, 0.01f)
        assertEquals(0.9f, tu5Specs.sparkPlugGapMm, 0.01f)
        assertTrue(tu5Specs.maintenanceTipsFa.isNotEmpty())
        assertEquals(ConfidenceLevel.VERIFIED_OEM, tu5Specs.confidence)

        val ef7Specs = VehicleSpecDatabase.getSpecSheet("ikco_dena_ef7")
        assertNotNull(ef7Specs)
        assertEquals(5.25f, ef7Specs!!.engineOilCapacityLiters, 0.01f)
        assertEquals(0.75f, ef7Specs.sparkPlugGapMm, 0.01f)

        val shahinSpecs = VehicleSpecDatabase.getSpecSheet("saipa_shahin_turbo")
        assertNotNull(shahinSpecs)
        assertEquals(4.0f, shahinSpecs!!.engineOilCapacityLiters, 0.01f)
    }
}
