package com.sarani.carobd.core.vehicle.parts

import com.sarani.carobd.core.model.CompatiblePartItem
import com.sarani.carobd.core.model.PartCategory
import com.sarani.carobd.core.model.PartDefinition

interface PartsRepository {
    fun getCategories(): List<PartCategory>
    fun getPartDefinition(id: String): PartDefinition?
    fun findCompatibleParts(
        vehicleMakeId: String,
        vehicleModelId: String,
        generationId: String? = null,
        engineId: String? = null,
        engineCode: String? = null,
        engineFamily: String? = null,
        modelYear: Int? = null,
        category: PartCategory? = null
    ): List<CompatiblePartItem>
    fun searchParts(query: String): List<CompatiblePartItem>
}
