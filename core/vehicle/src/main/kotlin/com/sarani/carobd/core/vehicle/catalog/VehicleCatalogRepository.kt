package com.sarani.carobd.core.vehicle.catalog

import com.sarani.carobd.core.model.EngineVariant
import com.sarani.carobd.core.model.ResolvedVehicleMatch
import com.sarani.carobd.core.model.TransmissionVariant
import com.sarani.carobd.core.model.VehicleGeneration
import com.sarani.carobd.core.model.VehicleMake
import com.sarani.carobd.core.model.VehicleModel
import com.sarani.carobd.core.model.VehicleVariant

/**
 * Normalized catalog repository providing structured, provenance-aware vehicle definitions.
 *
 * Distinguishes makes, models, generations, engine families, transmissions, and variants
 * without relying on unstructured string guessing or hardcoded UI assumptions.
 */
interface VehicleCatalogRepository {
    fun getAllMakes(): List<VehicleMake>
    fun getMakeById(id: String): VehicleMake?

    fun getModelsForMake(makeId: String): List<VehicleModel>
    fun getModelById(id: String): VehicleModel?

    fun getGenerationsForModel(modelId: String): List<VehicleGeneration>
    fun getGenerationById(id: String): VehicleGeneration?

    fun getVariantsForGeneration(generationId: String): List<VehicleVariant>
    fun getVariantById(id: String): VehicleVariant?

    fun getEngineById(id: String): EngineVariant?
    fun getTransmissionById(id: String): TransmissionVariant?

    fun searchMakes(query: String): List<VehicleMake>
    fun searchModels(query: String, makeId: String? = null): List<VehicleModel>

    /**
     * Resolves matching variants for a given make, model, optional year, and optional engine code.
     * Respects model year production boundaries and engine family truthfulness.
     */
    fun findMatchingVariants(
        makeQuery: String,
        modelQuery: String,
        modelYear: Int? = null,
        engineQuery: String? = null
    ): List<ResolvedVehicleMatch>
}
