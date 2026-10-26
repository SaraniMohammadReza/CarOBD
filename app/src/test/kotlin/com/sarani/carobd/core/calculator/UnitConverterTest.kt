package com.sarani.carobd.core.calculator

import com.sarani.carobd.core.storage.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `100 kmh approximately 62_1 mph`() {
        assertEquals(62.1371, UnitConverter.kmhToMph(100.0), 0.01)
    }

    @Test
    fun `0 celsius equals 32 fahrenheit`() {
        assertEquals(32.0, UnitConverter.celsiusToFahrenheit(0.0), 0.001)
    }

    @Test
    fun `100 celsius equals 212 fahrenheit`() {
        assertEquals(212.0, UnitConverter.celsiusToFahrenheit(100.0), 0.001)
    }

    @Test
    fun `100 kPa approximately 14_5 psi`() {
        assertEquals(14.5038, UnitConverter.kpaToPsi(100.0), 0.01)
    }

    @Test
    fun `formatSpeed metric renders kmh`() {
        val (value, unit) = UnitConverter.formatSpeed(100.0, UnitSystem.METRIC)
        assertEquals("100.0", value)
        assertEquals("km/h", unit)
    }

    @Test
    fun `formatSpeed imperial renders mph`() {
        val (value, unit) = UnitConverter.formatSpeed(100.0, UnitSystem.IMPERIAL)
        assertEquals("62.1", value)
        assertEquals("mph", unit)
    }

    @Test
    fun `formatDistance metric renders km`() {
        val (value, unit) = UnitConverter.formatDistance(100.0, UnitSystem.METRIC)
        assertEquals("100.0", value)
        assertEquals("km", unit)
    }

    @Test
    fun `formatDistance imperial renders miles`() {
        val (value, unit) = UnitConverter.formatDistance(100.0, UnitSystem.IMPERIAL)
        assertEquals("62.1", value)
        assertEquals("mi", unit)
    }

    @Test
    fun `unknown NaN renders em dash not zero`() {
        val (value, _) = UnitConverter.formatSpeed(Double.NaN, UnitSystem.METRIC)
        assertEquals("—", value)
    }

    @Test
    fun `round trip kmh to mph and back`() {
        assertEquals(
            100.0,
            UnitConverter.mphToKmh(UnitConverter.kmhToMph(100.0)),
            0.001
        )
    }
}
