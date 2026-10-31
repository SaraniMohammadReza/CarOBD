package com.sarani.carobd.core.vehicle.parts

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.PartCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PartsRepositoryTest {

    private lateinit var repository: DefaultPartsRepository

    @Before
    fun setUp() {
        repository = DefaultPartsRepository()
    }

    @Test
    fun getCategoriesReturnsDistinctCategories() {
        val categories = repository.getCategories()
        assertTrue(categories.isNotEmpty())
        assertTrue(categories.contains(PartCategory.SPARK_PLUG))
        assertTrue(categories.contains(PartCategory.TIMING_BELT))
        assertTrue(categories.contains(PartCategory.OIL_FILTER))
        assertEquals(categories.size, categories.distinct().size)
    }

    @Test
    fun tondar90K4mReturnsVerifiedOemPartsWithCorrectSpecs() {
        val parts = repository.findCompatibleParts(
            vehicleMakeId = "renault",
            vehicleModelId = "renault_l90",
            generationId = "gen_l90",
            engineId = "eng_k4m",
            engineCode = "K4M 690",
            engineFamily = "K-Type",
            modelYear = 2012
        )

        assertFalse(parts.isEmpty())

        val sparkPlug = parts.firstOrNull { it.definition.category == PartCategory.SPARK_PLUG }
        assertNotNull(sparkPlug)
        assertEquals(ConfidenceLevel.VERIFIED_OEM, sparkPlug!!.confidence)
        assertTrue(sparkPlug.isOemVerified)
        assertTrue(sparkPlug.fitment.oemNumbers.any { it.partNumber == "77 00 500 168" })

        val timingBelt = parts.firstOrNull { it.definition.category == PartCategory.TIMING_BELT }
        assertNotNull(timingBelt)
        assertEquals(ConfidenceLevel.VERIFIED_OEM, timingBelt!!.confidence)
        assertTrue(timingBelt.fitment.oemNumbers.any { it.partNumber == "13 0C 175 29R" })
        assertEquals("132", timingBelt.fitment.specifications.first { it.key == "teeth" }.value)
    }

    @Test
    fun engineMismatchRejectionPreventsDangerousPartCrossApplication() {
        // Querying Peugeot 206 with TU3 engine
        val tu3Parts = repository.findCompatibleParts(
            vehicleMakeId = "ikco",
            vehicleModelId = "ikco_206",
            generationId = "gen_206_ir",
            engineId = "eng_tu3",
            engineCode = "TU3 A/JP",
            engineFamily = "TU",
            modelYear = 2010,
            category = PartCategory.TIMING_BELT
        )

        assertEquals(1, tu3Parts.size)
        val tu3Belt = tu3Parts.first()
        // TU3 timing belt has 104/108 teeth, NOT 134 teeth
        assertEquals("104/108", tu3Belt.fitment.specifications.first { it.key == "teeth" }.value)
        assertFalse(tu3Belt.fitment.specifications.any { it.value == "134" })

        // Querying Peugeot 206 with TU5 engine
        val tu5Parts = repository.findCompatibleParts(
            vehicleMakeId = "ikco",
            vehicleModelId = "ikco_206",
            generationId = "gen_206_ir",
            engineId = "eng_tu5",
            engineCode = "TU5 JP4",
            engineFamily = "TU",
            modelYear = 2010,
            category = PartCategory.TIMING_BELT
        )

        assertEquals(1, tu5Parts.size)
        val tu5Belt = tu5Parts.first()
        // TU5 timing belt has 134 teeth, NOT 104/108 teeth
        assertEquals("134", tu5Belt.fitment.specifications.first { it.key == "teeth" }.value)
        assertFalse(tu5Belt.fitment.specifications.any { it.value == "104/108" })
    }

    @Test
    fun unknownEngineRejectsEngineSpecificSafetyFitments() {
        // When vehicle engine is unknown, timing belt or spark plug must NOT be guessed
        val partsWithoutEngine = repository.findCompatibleParts(
            vehicleMakeId = "ikco",
            vehicleModelId = "ikco_206",
            generationId = "gen_206_ir",
            engineId = null,
            engineCode = null,
            modelYear = 2015,
            category = PartCategory.TIMING_BELT
        )

        // Safety invariant: empty list, never guess across engines
        assertTrue(partsWithoutEngine.isEmpty())
    }

    @Test
    fun modelYearBoundariesAreStrictlyRespected() {
        // Tondar 90 fitment minYear is 2007, maxYear is 2019
        val beforeProduction = repository.findCompatibleParts(
            vehicleMakeId = "renault",
            vehicleModelId = "renault_l90",
            generationId = "gen_l90",
            engineId = "eng_k4m",
            modelYear = 2002
        )
        assertTrue(beforeProduction.isEmpty())

        val afterProduction = repository.findCompatibleParts(
            vehicleMakeId = "renault",
            vehicleModelId = "renault_l90",
            generationId = "gen_l90",
            engineId = "eng_k4m",
            modelYear = 2025
        )
        assertTrue(afterProduction.isEmpty())

        val validYear = repository.findCompatibleParts(
            vehicleMakeId = "renault",
            vehicleModelId = "renault_l90",
            generationId = "gen_l90",
            engineId = "eng_k4m",
            modelYear = 2015
        )
        assertFalse(validYear.isEmpty())
    }

    @Test
    fun confidenceLevelPreservationEnsuresTruthfulProvenance() {
        // Tondar 90 is verified OEM
        val l90Parts = repository.findCompatibleParts(
            vehicleMakeId = "renault",
            vehicleModelId = "renault_l90",
            generationId = "gen_l90",
            engineId = "eng_k4m"
        )
        assertTrue(l90Parts.all { it.confidence == ConfidenceLevel.VERIFIED_OEM })

        // Dena EF7 is community confirmed
        val denaParts = repository.findCompatibleParts(
            vehicleMakeId = "ikco",
            vehicleModelId = "ikco_dena",
            generationId = "gen_dena",
            engineId = "eng_ef7_na"
        )
        assertTrue(denaParts.all { it.confidence == ConfidenceLevel.COMMUNITY_CONFIRMED })
        assertFalse(denaParts.any { it.isOemVerified })
    }

    @Test
    fun searchPartsByOemNumberAndBrandFindsMatches() {
        val oemMatches = repository.searchParts("77 00 500 168")
        assertFalse(oemMatches.isEmpty())
        assertEquals("part_spark_plug", oemMatches.first().definition.id)

        val brandMatches = repository.searchParts("Eyquem")
        assertFalse(brandMatches.isEmpty())

        val persianMatches = repository.searchParts("شمع")
        assertFalse(persianMatches.isEmpty())
    }
}
