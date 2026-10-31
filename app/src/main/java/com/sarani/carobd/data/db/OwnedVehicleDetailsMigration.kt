package com.sarani.carobd.data.db

/** Published v5 remains immutable; optional ownership details are added in v6. */
object OwnedVehicleDetailsMigration {
    val version6 = listOf(
        "ALTER TABLE owned_vehicles ADD COLUMN fuel_type TEXT",
        "ALTER TABLE owned_vehicles ADD COLUMN notes TEXT",
    )
}
