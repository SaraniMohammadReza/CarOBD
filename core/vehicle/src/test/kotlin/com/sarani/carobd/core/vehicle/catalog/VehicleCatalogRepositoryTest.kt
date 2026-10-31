package com.sarani.carobd.core.vehicle.catalog

import com.sarani.carobd.core.model.AspirationType
import com.sarani.carobd.core.model.ConfidenceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleCatalogRepositoryTest {

    private val repo: VehicleCatalogRepository = DefaultVehicleCatalogRepository()

    @Test
    fun testGetAllMakesAndLookupById() {
        val makes = repo.getAllMakes()
        assertTrue("Should contain at least IKCO, SAIPA, Peugeot, Renault", makes.size >= 4)

        val ikco = repo.getMakeById("ikco")
        assertNotNull(ikco)
        assertEquals("Iran Khodro", ikco!!.name)

        val nonExistent = repo.getMakeById("unknown_make")
        assertNull(nonExistent)
    }

    @Test
    fun testSearchMakesWithEnglishAndPersianAliases() {
        val persianMatch = repo.searchMakes("ایران خودرو")
        assertTrue(persianMatch.any { it.id == "ikco" })

        val persianNoZwnjMatch = repo.searchMakes("ایران‌خودرو")
        assertTrue(persianNoZwnjMatch.any { it.id == "ikco" })

        val englishMatch = repo.searchMakes("peugeot")
        assertTrue(englishMatch.any { it.id == "peugeot" })

        val saipaMatch = repo.searchMakes("سایپا")
        assertTrue(saipaMatch.any { it.id == "saipa" })
    }

    @Test
    fun testSearchModelsWithEnglishAndPersianAliases() {
        val models206 = repo.searchModels("پژو ۲۰۶")
        assertTrue(models206.any { it.id == "ikco_206" })

        val denaModels = repo.searchModels("دنا")
        assertTrue(denaModels.any { it.id == "ikco_dena" })

        val shahinModels = repo.searchModels("شاهین")
        assertTrue(shahinModels.any { it.id == "saipa_shahin" })

        val l90Models = repo.searchModels("ال ۹۰")
        assertTrue(l90Models.any { it.id == "renault_l90" })
    }

    @Test
    fun testFindMatchingVariantsDifferentiatesEngineFamilies() {
        // IKCO 206 with TU5 engine
        val tu5Matches = repo.findMatchingVariants(
            makeQuery = "ایران خودرو",
            modelQuery = "۲۰۶",
            modelYear = 2015,
            engineQuery = "TU5"
        )
        assertEquals(1, tu5Matches.size)
        val tu5 = tu5Matches.first()
        assertEquals("var_206_type5", tu5.variant.id)
        assertEquals("eng_tu5", tu5.engine.id)
        assertEquals(1587, tu5.engine.displacementCc)
        assertEquals(ConfidenceLevel.COMMUNITY_CONFIRMED, tu5.confidence)

        // IKCO 206 with TU3 engine (same model, different engine family)
        val tu3Matches = repo.findMatchingVariants(
            makeQuery = "IKCO",
            modelQuery = "206",
            modelYear = 2015,
            engineQuery = "TU3"
        )
        assertEquals(1, tu3Matches.size)
        val tu3 = tu3Matches.first()
        assertEquals("var_206_type2", tu3.variant.id)
        assertEquals("eng_tu3", tu3.engine.id)
        assertEquals(1360, tu3.engine.displacementCc)

        // Verifies that TU5 and TU3 are never conflated
        assertTrue(tu5.variant.id != tu3.variant.id)
        assertTrue(tu5.engine.id != tu3.engine.id)
    }

    @Test
    fun testModelYearBoundariesEnforced() {
        // 206 Type 5 was produced 2003..2022
        val tooEarly = repo.findMatchingVariants(
            makeQuery = "IKCO",
            modelQuery = "206",
            modelYear = 1999,
            engineQuery = "TU5"
        )
        assertTrue("Year 1999 precedes 206 Type 5 start year", tooEarly.isEmpty())

        // Dena+ Turbo Auto started in 2020
        val denaTooEarly = repo.findMatchingVariants(
            makeQuery = "IKCO",
            modelQuery = "دنا",
            modelYear = 2012,
            engineQuery = "EF7 TC"
        )
        assertTrue("Year 2012 precedes Dena+ Turbo start year", denaTooEarly.isEmpty())

        val validDenaTurbo = repo.findMatchingVariants(
            makeQuery = "IKCO",
            modelQuery = "دنا",
            modelYear = 2021,
            engineQuery = "EF7 TC"
        )
        assertEquals(1, validDenaTurbo.size)
        assertEquals("var_dena_plus_turbo_at", validDenaTurbo.first().variant.id)
    }

    @Test
    fun testSpecificationsAndProvenancePreserved() {
        val denaVariant = repo.getVariantById("var_dena_ef7_na")
        assertNotNull(denaVariant)
        val specs = denaVariant!!.specifications
        assertNotNull(specs)
        assertEquals(5.25f, specs!!.engineOilCapacityLiters!!, 0.01f)
        assertEquals(0.75f, specs.sparkPlugGapMm!!, 0.01f)
        assertEquals(60000, specs.timingBeltIntervalKm)
        assertEquals(ConfidenceLevel.COMMUNITY_CONFIRMED, denaVariant.confidence)

        val engine = repo.getEngineById(denaVariant.engineId)
        assertNotNull(engine)
        assertTrue("Engine must preserve sources", engine!!.sources.isNotEmpty())
        assertEquals("IPCO", engine.sources.first().manufacturer)
    }

    @Test
    fun testUnverifiedRecordsCannotBeSilentlyPromotedToVerifiedOem() {
        // §Forensic Audit: Catalog data must not claim VERIFIED_OEM without verified OEM-signed source chain
        val tu5Variant = repo.getVariantById("var_206_type5")!!
        assertNotEquals("Community technical data must NOT be falsely marked VERIFIED_OEM",
            ConfidenceLevel.VERIFIED_OEM, tu5Variant.confidence)
        assertEquals(ConfidenceLevel.COMMUNITY_CONFIRMED, tu5Variant.confidence)

        val tu5Engine = repo.getEngineById("eng_tu5")!!
        assertNotEquals(ConfidenceLevel.VERIFIED_OEM, tu5Engine.confidence)
        assertEquals(ConfidenceLevel.COMMUNITY_CONFIRMED, tu5Engine.confidence)

        // Sources must not contain synthesized/fake document IDs
        tu5Engine.sources.forEach { source ->
            assertTrue("urlOrDocument should be empty or genuine, not a synthesized fake ID",
                source.urlOrDocument.isBlank() || source.urlOrDocument == "MR388")
        }
    }

    @Test
    fun testMissingSpecificationsRemainStrictlyNull() {
        val type2 = repo.getVariantById("var_206_type2")!!
        val specs = type2.specifications!!

        // Unmeasured or unverified specifications must not be populated with generic defaults
        assertNull("Missing tireFront must remain null", specs.tireFront)
        assertNull("Missing tireRear must remain null", specs.tireRear)
        assertNull("Missing batterySpec must remain null", specs.batterySpec)
        assertNull("Missing brakeFluidType must remain null", specs.brakeFluidType)
    }

    @Test
    fun testEngineVariantDataCannotLeakAcrossVariants() {
        val type2Specs = repo.getVariantById("var_206_type2")!!.specifications!!
        val type5Specs = repo.getVariantById("var_206_type5")!!.specifications!!

        // TU3 has 108 teeth belt and 3.25L capacity; TU5 has 134 teeth belt and 3.75L capacity
        assertNotEquals(type2Specs.timingBeltTeeth, type5Specs.timingBeltTeeth)
        assertEquals(108, type2Specs.timingBeltTeeth)
        assertEquals(134, type5Specs.timingBeltTeeth)

        assertNotEquals(type2Specs.engineOilCapacityLiters, type5Specs.engineOilCapacityLiters)
        assertEquals(3.25f, type2Specs.engineOilCapacityLiters!!, 0.01f)
        assertEquals(3.75f, type5Specs.engineOilCapacityLiters!!, 0.01f)

        // Dena EF7 NA vs Dena+ Turbo
        val denaNaSpecs = repo.getVariantById("var_dena_ef7_na")!!.specifications!!
        val denaTurboSpecs = repo.getVariantById("var_dena_plus_turbo_at")!!.specifications!!
        val denaNaEngine = repo.getEngineById("eng_ef7_na")!!
        val denaTurboEngine = repo.getEngineById("eng_ef7_tc")!!

        assertEquals(AspirationType.NATURALLY_ASPIRATED, denaNaEngine.aspiration)
        assertEquals(AspirationType.TURBOCHARGED, denaTurboEngine.aspiration)
        assertNotEquals(denaNaSpecs.sparkPlugGapMm, denaTurboSpecs.sparkPlugGapMm)
        assertEquals(0.75f, denaNaSpecs.sparkPlugGapMm!!, 0.01f)
        assertEquals(0.70f, denaTurboSpecs.sparkPlugGapMm!!, 0.01f)
    }

    @Test
    fun testFieldLevelProvenanceIsolation() {
        // Tondar 90 has genuine MR388 OEM manual for K4M
        val tondar = repo.getVariantById("var_tondar90_e2")!!
        val tondarSpecs = tondar.specifications!!

        val oilProv = tondarSpecs.getFieldProvenance<Float>("engineOilCapacityLiters")
        assertNotNull("Tondar oil capacity must have field provenance", oilProv)
        assertTrue("Tondar oil capacity with MR388 manual is OEM verified", oilProv!!.isOemVerified())
        assertEquals(ConfidenceLevel.VERIFIED_OEM, oilProv.confidence)
        assertEquals("MR388", oilProv.source?.urlOrDocument)
        assertTrue(oilProv.isSafeForAutomaticRecommendation)

        // 206 Type 5 has community data
        val tu5 = repo.getVariantById("var_206_type5")!!
        val tu5Specs = tu5.specifications!!

        val tu5OilProv = tu5Specs.getFieldProvenance<Float>("engineOilCapacityLiters")
        assertNotNull("TU5 oil capacity must have field provenance", tu5OilProv)
        org.junit.Assert.assertFalse("TU5 oil capacity is not OEM verified", tu5OilProv!!.isOemVerified())
        assertEquals(ConfidenceLevel.COMMUNITY_CONFIRMED, tu5OilProv.confidence)

        // One field having an OEM source on Tondar 90 must not promote a missing field
        assertNull(tondarSpecs.getFieldProvenance<String>("tireFront"))
        org.junit.Assert.assertFalse(tondarSpecs.isFieldSafeForRecommendation("tireFront"))
        assertEquals(ConfidenceLevel.UNKNOWN, tondarSpecs.getFieldConfidence("tireFront"))
    }

    @Test
    fun testCommunityDataCannotTriggerOemLabel() {
        val dena = repo.getVariantById("var_dena_ef7_na")!!
        val denaSpecs = dena.specifications!!

        val beltIntervalProv = denaSpecs.getFieldProvenance<Int>("timingBeltIntervalKm")
        assertNotNull(beltIntervalProv)
        org.junit.Assert.assertFalse("Community interval must never be OEM verified", beltIntervalProv!!.isOemVerified())
        assertNotEquals(ConfidenceLevel.VERIFIED_OEM, beltIntervalProv.confidence)
        assertEquals(ConfidenceLevel.COMMUNITY_CONFIRMED, beltIntervalProv.confidence)
    }

    @Test
    fun testUncertainIntervalCannotBeMandatoryOemSchedule() {
        val shahin = repo.getVariantById("var_shahin_g")!!
        val shahinSpecs = shahin.specifications!!

        val beltProv = shahinSpecs.getFieldProvenance<Int>("timingBeltIntervalKm")
        assertNotNull(beltProv)
        org.junit.Assert.assertFalse("Uncertain community interval cannot be marked safe for automatic mandatory recommendation",
            beltProv!!.isSafeForAutomaticRecommendation)

        // But physical verified tooth count can be trusted for part verification
        val teethProv = shahinSpecs.getFieldProvenance<Int>("timingBeltTeeth")
        assertNotNull(teethProv)
        assertTrue(teethProv!!.isSafeForAutomaticRecommendation)
    }
}
