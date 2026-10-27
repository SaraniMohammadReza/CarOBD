package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.PidCapabilityMap
import com.sarani.carobd.core.model.ReadinessMonitor
import com.sarani.carobd.core.model.ReadinessStatus

/**
 * SAE J1979 Diagnostic PID Codec.
 * Handles encoding, decoding, bitmask evaluation, and physical scaling conversions.
 */
object PidCodec {

    /**
     * Cleans an ELM327 / OBD hex response string into raw bytes.
     * Removes spaces, carriage returns, echoes, and prompt markers.
     */
    fun cleanResponseHex(response: String): ByteArray {
        val clean = response.replace(Regex("[^0-9A-Fa-f]"), "")
        if (clean.length % 2 != 0 || clean.isEmpty()) return ByteArray(0)

        val bytes = ByteArray(clean.length / 2)
        for (i in bytes.indices) {
            val hexByte = clean.substring(i * 2, i * 2 + 2)
            bytes[i] = hexByte.toInt(16).toByte()
        }
        return bytes
    }

    /**
     * Checks if response has expected service ID and PID echo.
     * E.g. for Mode 01 PID 0C, response bytes should begin with [0x41, 0x0C].
     */
    fun verifyResponseHeader(bytes: ByteArray, expectedModeEcho: Int, expectedPid: Int): Boolean {
        if (bytes.size < 2) return false
        val modeEcho = bytes[0].toInt() and 0xFF
        val pidEcho = bytes[1].toInt() and 0xFF
        return modeEcho == expectedModeEcho && pidEcho == expectedPid
    }

    /**
     * §10: Mode 04 (Clear DTC) response validation. A positive response echoes
     * service id 0x44 with no DTC payload. An empty buffer or a non-0x44 echo is
     * NOT a successful clear — never fabricate success from failed communication.
     */
    fun verifyClearDtcResponse(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        return (bytes[0].toInt() and 0xFF) == 0x44
    }

    // -------------------------------------------------------------
    // MODE 01 SUPPORTED BITMASKS (00, 20, 40, 60, 80, A0, C0)
    // -------------------------------------------------------------

    /**
     * Extracts 32 supported PIDs from a 4-byte bitmask response.
     * @param bytes 4 data bytes corresponding to the bitmask
     * @param basePid The base PID (0x00, 0x20, 0x40, 0x60, 0x80, 0xA0)
     */
    fun decodeSupportedPids(bytes: ByteArray, basePid: Int): Set<Int> {
        val supported = mutableSetOf<Int>()
        if (bytes.size < 4) return supported

        for (byteIdx in 0 until 4) {
            val b = bytes[byteIdx].toInt() and 0xFF
            for (bit in 7 downTo 0) {
                val isSupported = (b and (1 shl bit)) != 0
                if (isSupported) {
                    val pidOffset = (byteIdx * 8) + (7 - bit) + 1
                    supported.add(basePid + pidOffset)
                }
            }
        }
        return supported
    }

    /**
     * Decodes Mode 01 PID 01 (Monitor status since DTCs cleared).
     */
    fun decodeReadinessStatus(data: ByteArray): ReadinessStatus {
        if (data.size < 4) return ReadinessStatus(milActive = false, dtcCount = 0, monitors = emptyList())

        val a = data[0].toInt() and 0xFF
        val b = data[1].toInt() and 0xFF
        val c = data[2].toInt() and 0xFF
        val d = data[3].toInt() and 0xFF

        val milOn = (a and 0x80) != 0
        val dtcCount = a and 0x7F

        val monitors = mutableListOf<ReadinessMonitor>()

        // Continuous monitors (Byte B)
        monitors.add(
            ReadinessMonitor(
                id = "MISFIRE",
                nameEn = "Misfire Monitor",
                nameFa = "پایش احتراق ناقص (Misfire)",
                isSupported = (b and 0x01) != 0,
                isComplete = (b and 0x10) == 0
            )
        )
        monitors.add(
            ReadinessMonitor(
                id = "FUEL_SYS",
                nameEn = "Fuel System",
                nameFa = "سامانه سوخت‌رسانی",
                isSupported = (b and 0x02) != 0,
                isComplete = (b and 0x20) == 0
            )
        )
        monitors.add(
            ReadinessMonitor(
                id = "COMPONENTS",
                nameEn = "Comprehensive Components",
                nameFa = "پایش جامع قطعات الکترونیکی",
                isSupported = (b and 0x04) != 0,
                isComplete = (b and 0x40) == 0
            )
        )

        // Non-continuous monitors (Byte C & D)
        val isDiesel = (b and 0x08) != 0
        if (!isDiesel) {
            monitors.add(ReadinessMonitor("CATALYST", "Catalyst Monitor", "پایش کاتالیزور", isSupported = (c and 0x01) != 0, isComplete = (d and 0x01) == 0))
            monitors.add(ReadinessMonitor("HEATED_CAT", "Heated Catalyst", "گرمکن کاتالیزور", isSupported = (c and 0x02) != 0, isComplete = (d and 0x02) == 0))
            monitors.add(ReadinessMonitor("EVAP", "Evaporative System", "سیستم جذب بخارات بنزین (کنیستر)", isSupported = (c and 0x04) != 0, isComplete = (d and 0x04) == 0))
            monitors.add(ReadinessMonitor("SECONDARY_AIR", "Secondary Air System", "سیستم هوای ثانویه", isSupported = (c and 0x08) != 0, isComplete = (d and 0x08) == 0))
            monitors.add(ReadinessMonitor("O2_SENSOR", "Oxygen Sensor", "سنسور اکسیژن", isSupported = (c and 0x20) != 0, isComplete = (d and 0x20) == 0))
            monitors.add(ReadinessMonitor("O2_HEATER", "O2 Sensor Heater", "گرمکن سنسور اکسیژن", isSupported = (c and 0x40) != 0, isComplete = (d and 0x40) == 0))
            monitors.add(ReadinessMonitor("EGR_VVT", "EGR / VVT System", "سیستم بازچرخانی گاز اگزوز / زمان‌بندی متغیر سوپاپ", isSupported = (c and 0x80) != 0, isComplete = (d and 0x80) == 0))
        } else {
            monitors.add(ReadinessMonitor("NMHC_CAT", "NMHC Catalyst", "کاتالیست NMHC دیزل", isSupported = (c and 0x01) != 0, isComplete = (d and 0x01) == 0))
            monitors.add(ReadinessMonitor("NOX_SCR", "NOx / SCR Aftertreatment", "سامانه کاهش آلایندگی SCR/NOx", isSupported = (c and 0x02) != 0, isComplete = (d and 0x02) == 0))
            monitors.add(ReadinessMonitor("BOOST", "Boost Pressure System", "سیستم فشار توربوشارژر", isSupported = (c and 0x08) != 0, isComplete = (d and 0x08) == 0))
            monitors.add(ReadinessMonitor("EXHAUST_GAS", "Exhaust Gas Sensor", "سنسور گازهای اگزوز", isSupported = (c and 0x20) != 0, isComplete = (d and 0x20) == 0))
            monitors.add(ReadinessMonitor("PM_FILTER", "PM Filter (DPF)", "فیلتر ذرات معلق دیزل (DPF)", isSupported = (c and 0x40) != 0, isComplete = (d and 0x40) == 0))
            monitors.add(ReadinessMonitor("EGR_VVT", "EGR / VVT System", "سیستم EGR دیزل", isSupported = (c and 0x80) != 0, isComplete = (d and 0x80) == 0))
        }

        return ReadinessStatus(milActive = milOn, dtcCount = dtcCount, monitors = monitors)
    }

    // -------------------------------------------------------------
    // PHYSICAL METRIC DECODERS (MODE 01)
    // -------------------------------------------------------------

    /** Engine RPM: ((A * 256) + B) / 4 */
    fun decodeEngineRpm(a: Int, b: Int): Double = ((a * 256) + b) / 4.0

    /** Vehicle Speed: A (km/h) */
    fun decodeVehicleSpeed(a: Int): Double = a.toDouble()

    /** Coolant / Intake / Ambient / Oil Temp: A - 40 (°C) */
    fun decodeStandardTemperature(a: Int): Double = (a - 40).toDouble()

    /** Calculated Engine Load: (A * 100) / 255 (%) */
    fun decodeEngineLoad(a: Int): Double = (a * 100.0) / 255.0

    /** Short / Long Term Fuel Trim: ((A - 128) * 100) / 128 (%) */
    fun decodeFuelTrim(a: Int): Double = ((a - 128) * 100.0) / 128.0

    /** Intake Manifold Absolute Pressure (MAP): A (kPa) */
    fun decodeMapPressure(a: Int): Double = a.toDouble()

    /** MAF Air Flow Rate: ((A * 256) + B) / 100 (g/s) */
    fun decodeMafAirFlow(a: Int, b: Int): Double = ((a * 256) + b) / 100.0

    /** Throttle Position: (A * 100) / 255 (%) */
    fun decodeThrottlePosition(a: Int): Double = (a * 100.0) / 255.0

    /** Timing Advance: (A - 128) / 2 (° before TDC) */
    fun decodeTimingAdvance(a: Int): Double = (a - 128) / 2.0

    /** Fuel Tank Level Input: (A * 100) / 255 (%) */
    fun decodeFuelLevel(a: Int): Double = (a * 100.0) / 255.0

    /** Fuel Rail Gauge Pressure: ((A * 256) + B) * 10 (kPa) */
    fun decodeFuelRailGaugePressure(a: Int, b: Int): Double = ((a * 256) + b) * 10.0

    /** Control Module Voltage: ((A * 256) + B) / 1000 (V) */
    fun decodeControlModuleVoltage(a: Int, b: Int): Double = ((a * 256) + b) / 1000.0

    /** Fuel-Air Equivalence Ratio (Lambda): ((A * 256) + B) / 32768 */
    fun decodeEquivalenceRatio(a: Int, b: Int): Double = ((a * 256) + b) / 32768.0

    /** Engine Fuel Rate: ((A * 256) + B) * 0.05 (L/h) */
    fun decodeEngineFuelRate(a: Int, b: Int): Double = ((a * 256) + b) * 0.05

    /** Run Time Since Engine Start: (A * 256) + B (seconds) */
    fun decodeEngineRunTime(a: Int, b: Int): Long = ((a * 256) + b).toLong()

    /** Distance Traveled with MIL: (A * 256) + B (km) */
    fun decodeDistanceWithMil(a: Int, b: Int): Double = ((a * 256) + b).toDouble()

    /**
     * Decodes a raw Mode 01 PID payload into a physical value.
     * Expects data bytes after the Mode (41) and PID echo bytes.
     */
    fun decodeMode01Metric(pid: Int, data: ByteArray): Double? {
        if (data.isEmpty()) return null
        val a = data[0].toInt() and 0xFF
        val b = if (data.size > 1) data[1].toInt() and 0xFF else 0

        return when (pid) {
            0x04 -> decodeEngineLoad(a)
            0x05 -> decodeStandardTemperature(a)
            0x06 -> decodeFuelTrim(a)
            0x07 -> decodeFuelTrim(a)
            0x08 -> decodeFuelTrim(a)
            0x09 -> decodeFuelTrim(a)
            0x0A -> (a * 3).toDouble()
            0x0B -> decodeMapPressure(a)
            0x0C -> decodeEngineRpm(a, b)
            0x0D -> decodeVehicleSpeed(a)
            0x0E -> decodeTimingAdvance(a)
            0x0F -> decodeStandardTemperature(a)
            0x10 -> decodeMafAirFlow(a, b)
            0x11 -> decodeThrottlePosition(a)
            0x1F -> decodeEngineRunTime(a, b).toDouble()
            0x21 -> decodeDistanceWithMil(a, b)
            0x22 -> ((a * 256) + b) * 0.079
            0x23 -> decodeFuelRailGaugePressure(a, b)
            0x2F -> decodeFuelLevel(a)
            0x31 -> decodeDistanceWithMil(a, b)
            0x33 -> a.toDouble()
            0x42 -> decodeControlModuleVoltage(a, b)
            0x43 -> ((a * 256) + b) * 100.0 / 255.0
            0x44 -> decodeEquivalenceRatio(a, b)
            0x45 -> decodeThrottlePosition(a)
            0x46 -> decodeStandardTemperature(a)
            0x4C -> decodeThrottlePosition(a)
            0x5C -> decodeStandardTemperature(a)
            0x5E -> decodeEngineFuelRate(a, b)
            else -> null
        }
    }

    // -------------------------------------------------------------
    // MODE 09 VEHICLE INFORMATION DECODERS
    // -------------------------------------------------------------

    /**
     * Decodes VIN from Mode 09 PID 02 response payload.
     * Handles ASCII bytes, filtering out unprintable characters or leading count bytes.
     */
    fun decodeVin(payload: ByteArray): String? {
        if (payload.isEmpty()) return null
        val sb = StringBuilder()

        for (byte in payload) {
            val charCode = byte.toInt() and 0xFF
            if (charCode in 0x20..0x7E) {
                sb.append(charCode.toChar())
            }
        }

        val cleaned = sb.toString().trim()
        val match = Regex("[A-HJ-NPR-Z0-9]{17}").find(cleaned)
        return match?.value ?: if (cleaned.length >= 10) cleaned else null
    }

    /**
     * Decodes Calibration ID (CALID) or CVN from Mode 09 payload.
     */
    fun decodeAsciiString(payload: ByteArray): String {
        val sb = StringBuilder()
        for (b in payload) {
            val code = b.toInt() and 0xFF
            if (code in 0x20..0x7E) {
                sb.append(code.toChar())
            }
        }
        return sb.toString().trim()
    }
}
