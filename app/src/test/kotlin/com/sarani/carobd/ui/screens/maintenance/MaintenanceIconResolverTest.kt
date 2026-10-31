package com.sarani.carobd.ui.screens.maintenance

import com.sarani.carobd.R
import com.sarani.carobd.core.model.MaintenanceCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class MaintenanceIconResolverTest {

    @Test
    fun testResolvesSpecificMaintenanceItemKeys() {
        assertEquals(R.drawable.ic_part_oil_can, MaintenanceIconResolver.getDrawableRes("engine_oil"))
        assertEquals(R.drawable.ic_part_oil_filter, MaintenanceIconResolver.getDrawableRes("oil_filter"))
        assertEquals(R.drawable.ic_part_air_filter, MaintenanceIconResolver.getDrawableRes("air_filter"))
        assertEquals(R.drawable.ic_part_cabin_filter, MaintenanceIconResolver.getDrawableRes("cabin_filter"))
        assertEquals(R.drawable.ic_part_fuel_filter, MaintenanceIconResolver.getDrawableRes("fuel_filter"))
        assertEquals(R.drawable.ic_part_spark_plug, MaintenanceIconResolver.getDrawableRes("spark_plugs"))
        assertEquals(R.drawable.ic_part_timing_belt, MaintenanceIconResolver.getDrawableRes("timing_belt"))
        assertEquals(R.drawable.ic_part_brake_disc, MaintenanceIconResolver.getDrawableRes("brake_pads"))
        assertEquals(R.drawable.ic_part_battery, MaintenanceIconResolver.getDrawableRes("battery"))
        assertEquals(R.drawable.ic_part_coolant, MaintenanceIconResolver.getDrawableRes("coolant"))
        assertEquals(R.drawable.ic_part_transmission, MaintenanceIconResolver.getDrawableRes("transmission_oil"))
        assertEquals(R.drawable.ic_part_transmission, MaintenanceIconResolver.getDrawableRes("gearbox_oil"))
    }

    @Test
    fun testResolvesCategoryFallbacksWhenItemKeyIsGeneric() {
        assertEquals(
            R.drawable.ic_part_oil_can,
            MaintenanceIconResolver.getDrawableRes("custom_inspection", MaintenanceCategory.ENGINE)
        )
        assertEquals(
            R.drawable.ic_part_transmission,
            MaintenanceIconResolver.getDrawableRes("custom_inspection", MaintenanceCategory.TRANSMISSION)
        )
        assertEquals(
            R.drawable.ic_part_brake_disc,
            MaintenanceIconResolver.getDrawableRes("custom_inspection", MaintenanceCategory.BRAKES)
        )
        assertEquals(
            R.drawable.ic_part_air_filter,
            MaintenanceIconResolver.getDrawableRes("custom_inspection", MaintenanceCategory.FILTERS)
        )
        assertEquals(
            R.drawable.ic_part_coolant,
            MaintenanceIconResolver.getDrawableRes("custom_inspection", MaintenanceCategory.FLUIDS)
        )
        assertEquals(
            R.drawable.ic_part_spark_plug,
            MaintenanceIconResolver.getDrawableRes("custom_inspection", MaintenanceCategory.IGNITION)
        )
        assertEquals(
            R.drawable.ic_part_battery,
            MaintenanceIconResolver.getDrawableRes("custom_inspection", MaintenanceCategory.ELECTRICAL)
        )
    }

    @Test
    fun testResolvesDefaultDrawableWhenUnknownAndNoCategory() {
        assertEquals(
            R.drawable.ic_part_oil_can,
            MaintenanceIconResolver.getDrawableRes("random_unmapped_service", null)
        )
    }
}
