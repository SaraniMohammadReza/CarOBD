package com.sarani.carobd.core.vehicle

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.VehicleType
import com.sarani.carobd.core.vehicle.catalog.VehicleCatalog
import com.sarani.carobd.core.vehicle.ecu.IranianEcuCatalog
import com.sarani.carobd.core.vehicle.specs.VehicleSpecDatabase
import org.junit.Assert.*
import org.junit.Test

class VehicleUnitTest {

    @Test
    fun testIranianEcuProfiles() {
        val ecuList = IranianEcuCatalog.allProfiles
        assertTrue("ECU catalog must have at least 10 profiles", ecuList.size >= 10)

        val me744 = IranianEcuCatalog.BOSCH_ME744_TU5
        assertEquals("10", me744.primaryAddressHex)
        assertNotNull(me744.protocolConfig.header)
        assertEquals("81", me744.protocolConfig.header?.priorityHex)
        assertEquals(ConfidenceLevel.VERIFIED_OEM, me744.confidence)

        val sim2k = IranianEcuCatalog.SIEMENS_SIM2K_XU7
        assertEquals("11", sim2k.primaryAddressHex)

        val me745 = IranianEcuCatalog.BOSCH_ME745_TU5
        assertTrue(me745.protocolConfig.standard.isCanBus)
    }

    @Test
    fun testVehicleCatalogCoverage() {
        val all = VehicleCatalog.allProfiles
        assertTrue(all.size >= 10)

        // Test filtering
        val cars = VehicleCatalog.filterByType(VehicleType.CAR)
        val motos = VehicleCatalog.filterByType(VehicleType.MOTORCYCLE)

        assertTrue(cars.isNotEmpty())
        assertTrue(motos.isNotEmpty())

        // Test search
        val ikaMatches = VehicleCatalog.search("Peugeot")
        assertTrue(ikaMatches.size >= 3)

        val saipaMatches = VehicleCatalog.search("Shahin")
        assertEquals(1, saipaMatches.size)
        assertEquals("saipa_shahin_turbo", saipaMatches[0].id)
        assertTrue(saipaMatches[0].engine.isTurbocharged)
    }

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
