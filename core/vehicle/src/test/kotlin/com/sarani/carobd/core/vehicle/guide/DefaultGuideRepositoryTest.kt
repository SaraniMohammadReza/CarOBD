package com.sarani.carobd.core.vehicle.guide

import com.sarani.carobd.core.model.GuideSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultGuideRepositoryTest {

    private val repository = DefaultGuideRepository()

    @Test
    fun getSectionsReturnsAllGuideSections() {
        val sections = repository.getSections()
        assertTrue(sections.isNotEmpty())
        assertTrue(sections.contains(GuideSection.GETTING_STARTED))
        assertTrue(sections.contains(GuideSection.OWNERSHIP))
        assertTrue(sections.contains(GuideSection.MAINTENANCE))
        assertTrue(sections.contains(GuideSection.DIAGNOSTICS))
    }

    @Test
    fun getAllTopicsReturnsPopulatedKnowledgeBase() {
        val topics = repository.getAllTopics()
        assertTrue("Repository must provide guide topics", topics.size >= 5)
        assertTrue(topics.any { it.id == "getting_started_adapter" })
        assertTrue(topics.any { it.id == "maintenance_timing_belt" })
        assertTrue(topics.any { it.id == "diagnostics_readonly" })
    }

    @Test
    fun getTopicsForSectionFiltersCorrectly() {
        val maintTopics = repository.getTopicsForSection(GuideSection.MAINTENANCE)
        assertTrue(maintTopics.isNotEmpty())
        assertTrue(maintTopics.all { it.section == GuideSection.MAINTENANCE })
    }

    @Test
    fun searchTopicsFindsByEnglishAndPersianQueries() {
        val enResult = repository.searchTopics("timing belt")
        assertTrue(enResult.isNotEmpty())
        assertTrue(enResult.any { it.id == "maintenance_timing_belt" })

        val faResult = repository.searchTopics("تسمه تایم")
        assertTrue(faResult.isNotEmpty())
        assertTrue(faResult.any { it.id == "maintenance_timing_belt" })

        val tu5Result = repository.searchTopics("tu5")
        assertTrue(tu5Result.isNotEmpty())
        assertTrue(tu5Result.any { it.id == "ownership_vehicle_specs" })
    }

    @Test
    fun getRelatedTopicsResolvesReferencedTopics() {
        val related = repository.getRelatedTopics("getting_started_adapter")
        assertTrue(related.isNotEmpty())
        assertTrue(related.any { it.id == "diagnostics_readonly" })
    }
}
