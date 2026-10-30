package com.sarani.carobd.ui.theme

import com.sarani.carobd.core.storage.*
import org.junit.Assert.*
import org.junit.Test

class AppearanceTest {
    @Test fun everyVisibleThemeHasDistinctRuntimePalette() {
        assertEquals(4, AppThemeMode.entries.map(::paletteFor).toSet().size)
        assertNotEquals(paletteFor(AppThemeMode.LIGHT_THEME).text, paletteFor(AppThemeMode.AMOLED_BLACK).text)
    }
    @Test fun imperialChangesValuesAndBoundsNotRawProtocolIdentifiers() {
        assertEquals(32.0, displayMeasurement(0.0, "°C", UnitSystem.IMPERIAL).value, 0.0)
        assertEquals("mph", displayMeasurement(100.0, "km/h", UnitSystem.IMPERIAL).unit)
        assertEquals(62.137119, displayMeasurement(100.0, "km/h", UnitSystem.IMPERIAL).value, 0.00001)
        assertEquals(DisplayMeasurement(12.4, "V"), displayMeasurement(12.4, "V", UnitSystem.IMPERIAL))
    }
}
