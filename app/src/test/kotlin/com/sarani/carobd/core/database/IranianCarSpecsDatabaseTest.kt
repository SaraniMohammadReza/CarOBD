package com.sarani.carobd.core.database

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.VehicleSpecSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IranianCarSpecsDatabaseTest {

    @Test
    fun classInitializationAndSpecsRetrievalSucceedsWithoutNoSuchMethodError() {
        // Trigger <clinit>
        val sheet206 = IranianCarSpecsDatabase.getSpecSheet("ikco_206_tu5")
        assertNotNull(sheet206)
        assertEquals("ikco_206_tu5", sheet206.vehicleId)
        assertEquals(ConfidenceLevel.COMMUNITY_CONFIRMED, sheet206.confidence)
        assertTrue(sheet206.timingBeltTeeth == 134)

        val sheetDena = IranianCarSpecsDatabase.getSpecSheet("ikco_dena_ef7")
        assertNotNull(sheetDena)
        assertEquals(5.5f, sheetDena.engineOilCapacityLiters)

        val fallback = IranianCarSpecsDatabase.getSpecSheet("non_existent_key")
        assertNotNull(fallback)
        assertEquals("global_generic", fallback.vehicleId)
    }

    @Test
    fun vehicleSpecSheetConstructorsBinaryCompatibility() {
        // 14-parameter constructor (legacy / default overloads compatibility)
        val sheet14 = VehicleSpecSheet(
            vehicleId = "test_14",
            engineCode = "TU5",
            engineOilGrade = "10W-40",
            engineOilCapacityLiters = 3.5f,
            recommendedOilBrands = listOf("Total"),
            gearboxOilGrade = "75W-80",
            gearboxCapacityLiters = 2.0f,
            sparkPlugModel = "FR7DC",
            sparkPlugGapMm = 0.9f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 134,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 6.0f,
            maintenanceTipsFa = listOf("Tip")
        )
        assertEquals("test_14", sheet14.vehicleId)
        assertEquals(ConfidenceLevel.VERIFIED_OEM, sheet14.confidence)
        assertTrue(sheet14.sources.isEmpty())

        // 16-parameter constructor
        val sheet16 = VehicleSpecSheet(
            vehicleId = "test_16",
            engineCode = "TU5",
            engineOilGrade = "10W-40",
            engineOilCapacityLiters = 3.5f,
            recommendedOilBrands = listOf("Total"),
            gearboxOilGrade = "75W-80",
            gearboxCapacityLiters = 2.0f,
            sparkPlugModel = "FR7DC",
            sparkPlugGapMm = 0.9f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 134,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 6.0f,
            maintenanceTipsFa = listOf("Tip"),
            sources = emptyList(),
            confidence = ConfidenceLevel.VERIFIED_OEM
        )
        assertEquals(ConfidenceLevel.VERIFIED_OEM, sheet16.confidence)
    }
}
