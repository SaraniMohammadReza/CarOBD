package com.sarani.carobd.core.model

data class GarageVehicle(
    val id: String,
    val profileId: String,
    val nickname: String,
    val manufacturer: String,
    val model: String,
    val year: Int,
    val engineCode: String,
    val fuelType: String,
    val licensePlate: String? = null,
    val vin: String? = null,
    val odometerKm: Int = 100000,
    val tankCapacityLiters: Float = 50f,
    val preferredAdapterAddress: String? = null,
    val preferredProtocolId: String? = null,
    val isDefault: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)
