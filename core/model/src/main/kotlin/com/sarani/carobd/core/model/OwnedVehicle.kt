package com.sarani.carobd.core.model

/**
 * A vehicle the user owns or manages.
 *
 * This is deliberately separate from [VehicleProfile], which configures diagnostic
 * routing. Optional facts remain null until the owner enters them or a provenance-aware
 * catalogue supplies them; selecting an owned vehicle must not change ECU routing.
 */
data class OwnedVehicle(
    val id: String,
    val displayName: String,
    val manufacturer: String? = null,
    val model: String? = null,
    val generation: String? = null,
    val year: Int? = null,
    val variant: String? = null,
    val engine: String? = null,
    val engineCode: String? = null,
    val transmission: String? = null,
    val market: String? = null,
    val vin: String? = null,
    val licensePlate: String? = null,
    val currentOdometerKm: Long? = null,
    val tankCapacityLiters: Double? = null,
    val createdAtMs: Long,
    val updatedAtMs: Long,
) {
    fun validationErrors(): List<String> = buildList {
        if (id.isBlank()) add("id must not be blank")
        if (displayName.isBlank()) add("displayName must not be blank")
        if (year != null && year !in 1886..2100) add("year is out of range")
        if (currentOdometerKm != null && currentOdometerKm < 0) add("odometer must not be negative")
        if (tankCapacityLiters != null && (!tankCapacityLiters.isFinite() || tankCapacityLiters <= 0.0)) {
            add("tank capacity must be positive and finite")
        }
        if (updatedAtMs < createdAtMs) add("updatedAtMs must not precede createdAtMs")
    }

    fun normalized(): OwnedVehicle = copy(
        displayName = displayName.trim(),
        manufacturer = manufacturer.nullIfBlank(),
        model = model.nullIfBlank(),
        generation = generation.nullIfBlank(),
        variant = variant.nullIfBlank(),
        engine = engine.nullIfBlank(),
        engineCode = engineCode.nullIfBlank(),
        transmission = transmission.nullIfBlank(),
        market = market.nullIfBlank(),
        vin = vin.nullIfBlank()?.uppercase(),
        licensePlate = licensePlate.nullIfBlank(),
    )
}

private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf(String::isNotEmpty)
