package com.sarani.carobd

import com.sarani.carobd.core.storage.VehicleSetupNumbers
import org.junit.Assert.*
import org.junit.Test

class VehicleSetupNumbersTest {
    @Test fun persianArabicAndEnglishDecimalKeyboardsRepresentTheSameCapacity() {
        for (input in listOf("50.5", "۵۰٫۵", "٥٠٫٥", " ۵0.٥ ")) assertEquals(input, 50.5f, VehicleSetupNumbers.tankLitres(input)!!, 0f)
    }
    @Test fun unenteredAndInvalidCapacityNeverBecomesAnAssumption() {
        for (input in listOf("", " ", "0", "۰", "-1", "NaN", "Infinity", "1e2", "50,5", "۵٬۰", "1.2.3", "9".repeat(64), "1".repeat(65), "5\t0")) {
            assertNull(input, VehicleSetupNumbers.tankLitres(input))
        }
    }
    @Test fun odometerPreservesRealZeroAndValidLocalizedIntegerBoundary() {
        assertEquals(0, VehicleSetupNumbers.odometerKm("۰"))
        assertEquals(123456, VehicleSetupNumbers.odometerKm("١۲3۴٥۶"))
        assertEquals(Int.MAX_VALUE, VehicleSetupNumbers.odometerKm("۲۱۴۷۴۸۳۶۴۷"))
    }
    @Test fun ambiguousFractionalOrOverflowOdometerIsRejectedWithoutRounding() {
        for (input in listOf("", "-1", "1.0", "۱٫۵", "۱٬۰۰۰", "1,000", "2147483648", "1e3")) assertNull(input, VehicleSetupNumbers.odometerKm(input))
    }
    @Test fun localizedModelYearIsOptionalAndStrict() {
        assertEquals(2024, VehicleSetupNumbers.modelYear("۲۰۲۴"))
        assertEquals(1999, VehicleSetupNumbers.modelYear("١٩٩٩"))
        for (input in listOf("", "24", "۲۰۲۴٫۰", "۱۸۰۰", "2101")) assertNull(VehicleSetupNumbers.modelYear(input))
    }
}
