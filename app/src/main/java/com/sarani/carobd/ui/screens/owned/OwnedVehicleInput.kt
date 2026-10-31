package com.sarani.carobd.ui.screens.owned

import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.storage.VehicleSetupNumbers
import java.util.UUID

/** Shared editor/onboarding boundary: optional blanks stay absent and supplied invalid numbers fail. */
internal data class OwnedVehicleInput(val vehicle: OwnedVehicle? = null, val error: OwnedVehicleEditorError? = null)
internal fun OwnedVehicleDraft.parse(now: Long = System.currentTimeMillis()): OwnedVehicleInput {
    val d = this
        val year = if (d.year.isBlank()) null else VehicleSetupNumbers.modelYear(d.year).also {
            if (it == null) { return OwnedVehicleInput(error=OwnedVehicleEditorError.YEAR_INVALID) }
        }
        val odometer = if (d.odometer.isBlank()) null else VehicleSetupNumbers.odometerKm(d.odometer)?.toLong().also {
            if (it == null) { return OwnedVehicleInput(error=OwnedVehicleEditorError.ODOMETER_INVALID) }
        }
        val tank = if (d.tankCapacity.isBlank()) null else VehicleSetupNumbers.tankLitres(d.tankCapacity)?.toDouble().also {
            if (it == null) { return OwnedVehicleInput(error=OwnedVehicleEditorError.TANK_INVALID) }
        }
        if (d.displayName.isBlank()) { return OwnedVehicleInput(error=OwnedVehicleEditorError.NAME_REQUIRED) }
        val vehicle = OwnedVehicle(d.id ?: UUID.randomUUID().toString(), d.displayName, d.manufacturer, d.model,
            d.generation, year, d.variant, d.engine, d.engineCode, d.transmission, d.fuelType, d.market, d.vin,
            d.licensePlate, odometer, tank, d.notes, d.createdAtMs ?: now, now).normalized()
    return OwnedVehicleInput(vehicle=vehicle)
}
