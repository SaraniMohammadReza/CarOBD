package com.sarani.carobd.ui.screens.maintenance

import androidx.annotation.DrawableRes
import com.sarani.carobd.R
import com.sarani.carobd.core.model.MaintenanceCategory

object MaintenanceIconResolver {

    @DrawableRes
    fun getDrawableRes(itemKey: String, category: MaintenanceCategory? = null): Int {
        val key = itemKey.lowercase().trim()
        return when {
            key.contains("oil_filter") -> R.drawable.ic_part_oil_filter
            key.contains("air_filter") -> R.drawable.ic_part_air_filter
            key.contains("cabin_filter") || key.contains("ac_filter") -> R.drawable.ic_part_cabin_filter
            key.contains("fuel_filter") -> R.drawable.ic_part_fuel_filter
            key.contains("filter") -> R.drawable.ic_part_oil_filter

            key.contains("gearbox") || key.contains("transmission") -> R.drawable.ic_part_transmission
            key.contains("oil") || key.contains("engine_oil") -> R.drawable.ic_part_oil_can
            key.contains("spark") || key.contains("plug") -> R.drawable.ic_part_spark_plug
            key.contains("belt") || key.contains("timing") -> R.drawable.ic_part_timing_belt
            key.contains("brake") || key.contains("pad") || key.contains("rotor") || key.contains("disc") -> R.drawable.ic_part_brake_disc
            key.contains("battery") || key.contains("accum") -> R.drawable.ic_part_battery
            key.contains("coolant") || key.contains("antifreeze") || key.contains("radiator") -> R.drawable.ic_part_coolant

            else -> when (category) {
                MaintenanceCategory.ENGINE -> R.drawable.ic_part_oil_can
                MaintenanceCategory.TRANSMISSION -> R.drawable.ic_part_transmission
                MaintenanceCategory.BRAKES -> R.drawable.ic_part_brake_disc
                MaintenanceCategory.FILTERS -> R.drawable.ic_part_air_filter
                MaintenanceCategory.FLUIDS -> R.drawable.ic_part_coolant
                MaintenanceCategory.IGNITION -> R.drawable.ic_part_spark_plug
                MaintenanceCategory.ELECTRICAL -> R.drawable.ic_part_battery
                else -> R.drawable.ic_part_oil_can
            }
        }
    }
}
