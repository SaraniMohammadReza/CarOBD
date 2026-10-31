package com.sarani.carobd.core.vehicle.guide

import com.sarani.carobd.core.model.GuideSection
import com.sarani.carobd.core.model.GuideTopic

interface GuideRepository {
    fun getSections(): List<GuideSection>
    fun getAllTopics(): List<GuideTopic>
    fun getTopicsForSection(section: GuideSection): List<GuideTopic>
    fun getTopicById(id: String): GuideTopic?
    fun searchTopics(query: String): List<GuideTopic>
    fun getRelatedTopics(topicId: String): List<GuideTopic>
}
