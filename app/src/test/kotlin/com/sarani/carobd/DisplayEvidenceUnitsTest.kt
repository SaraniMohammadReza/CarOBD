package com.sarani.carobd

import com.sarani.carobd.ui.theme.displayMeasurement
import com.sarani.carobd.core.storage.UnitSystem
import org.junit.Assert.*
import org.junit.Test

class DisplayEvidenceUnitsTest {
    @Test fun imperialFuelUsesExplicitUsGallonAndZeroEconomyIsUnknown() {
        val volume = displayMeasurement(3.785411784, "L", UnitSystem.IMPERIAL)
        assertEquals(1.0, volume.value, 1e-9); assertEquals("US gal", volume.unit)
        val economy = displayMeasurement(10.0, "L/100km", UnitSystem.IMPERIAL)
        assertEquals(23.5214583, economy.value, 1e-8); assertEquals("mpg (US)", economy.unit)
        assertTrue(displayMeasurement(0.0, "L/100km", UnitSystem.IMPERIAL).value.isNaN())
        assertEquals(0.0, displayMeasurement(0.0, "L", UnitSystem.IMPERIAL).value, 0.0)
    }
    @Test fun canonicalMetricValuesAndIdentifiersAreUnchanged() {
        assertEquals(10.0, displayMeasurement(10.0, "L/100km", UnitSystem.METRIC).value, 0.0)
        assertEquals(42.0, displayMeasurement(42.0, "raw", UnitSystem.IMPERIAL).value, 0.0)
        assertEquals(212.0, displayMeasurement(100.0, "°C", UnitSystem.IMPERIAL).value, 1e-9)
    }
}
