package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.CalibrationVerificationNumber
import com.sarani.carobd.core.model.Mode09Decoded
import com.sarani.carobd.core.model.PidCapabilityMap
import com.sarani.carobd.core.model.NegativeResponse
import com.sarani.carobd.core.model.ObdNrc
import com.sarani.carobd.core.model.ReadinessMonitor
import com.sarani.carobd.core.model.ReadinessStatus
import com.sarani.carobd.core.model.VehicleInformationResult

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
        require(data.size == 4) { "Readiness requires exactly four data bytes" }

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

        return ReadinessStatus(milActive = milOn, dtcCount = dtcCount, monitors = monitors, ignitionType = if (isDiesel) com.sarani.carobd.core.model.IgnitionType.Compression else com.sarani.carobd.core.model.IgnitionType.Spark)
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
        if (pid in setOf(0x0C, 0x10, 0x1F, 0x21, 0x22, 0x23, 0x31, 0x32, 0x3C, 0x3D, 0x3E, 0x3F, 0x42, 0x44, 0x5E) && data.size < 2) return null
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
        if (payload.size != 17 || payload.any { (it.toInt() and 0xFF) !in 0x20..0x7E }) return null
        return payload.toString(Charsets.US_ASCII).takeIf { it.matches(Regex("[A-HJ-NPR-Z0-9]{17}")) }
    }

    /**
     * Decodes a textual Mode 09 identifier (Calibration ID / CALID = PID 0904,
     * or ECU name = PID 090A) from ASCII bytes. Only CALID and ECU name are
     * textual; Calibration Verification Numbers (PID 0906) are binary 4-byte
     * values — use [decodeCvn] for those. Treating CVN as ASCII is a
     * truthfulness defect and is intentionally unsupported here.
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

    /**
     * §7 P0.7: CVN (Mode 09 PID 0906) is a sequence of 4-byte binary values —
     * NOT ASCII text. Each CVN is emitted as an 8-char hex string ("1234ABCD").
     * A trailing partial group (< 4 bytes) is malformed and dropped — never
     * fabricated or zero-padded. See [CalibrationVerificationNumber].
     */
    fun decodeCvn(payload: ByteArray): CalibrationVerificationNumber {
        val values = payload.toList().chunked(4) { chunk ->
            if (chunk.size < 4) null
            else chunk.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
        }.filterNotNull()
        return CalibrationVerificationNumber(values,
            quality = if (payload.isEmpty()) com.sarani.carobd.core.model.ReadingState.NoData else if (payload.size % 4 != 0) com.sarani.carobd.core.model.ReadingState.Partial else com.sarani.carobd.core.model.ReadingState.Success,
            trailingBytes = payload.takeLast(payload.size % 4).map { it.toInt() and 0xFF })
    }

    private fun mode09Result(
        pid: Int,
        payload: ByteArray,
        responder: String?,
        messageCount: Int?,
        multiFrameComplete: Boolean?,
        decoded: Mode09Decoded?
    ): VehicleInformationResult = VehicleInformationResult(
        pid = pid,
        responder = responder,
        rawBytes = payload.copyOf(),
        decoded = decoded,
        messageCount = messageCount,
        multiFrameComplete = multiFrameComplete
    )

    /** §7 P0.7: typed Mode 09 PID 0902 (VIN) result — preserves raw/responder/count. */
    fun decodeVinResult(
        payload: ByteArray,
        responder: String? = null,
        messageCount: Int? = null,
        multiFrameComplete: Boolean? = null
    ): VehicleInformationResult =
        mode09Result(
            pid = 0x02,
            payload = payload,
            responder = responder,
            messageCount = messageCount,
            multiFrameComplete = multiFrameComplete,
            decoded = if ((messageCount == null || messageCount == 1) && multiFrameComplete != false) decodeVin(payload)?.let { Mode09Decoded.Vin(it) } else null
        )

    /** §7 P0.7: typed Mode 09 PID 0904 (Calibration ID) — textual ASCII identifier. */
    fun decodeCalibrationIdResult(
        payload: ByteArray,
        responder: String? = null,
        messageCount: Int? = null,
        multiFrameComplete: Boolean? = null
    ): VehicleInformationResult =
        mode09Result(
            pid = 0x04,
            payload = payload,
            responder = responder,
            messageCount = messageCount,
            multiFrameComplete = multiFrameComplete,
            decoded = Mode09Decoded.CalibrationId(decodeAsciiString(payload))
        )

    /** §7 P0.7: typed Mode 09 PID 0906 (CVN) — typed 4-byte binary values. */
    fun decodeCvnResult(
        payload: ByteArray,
        responder: String? = null,
        messageCount: Int? = null,
        multiFrameComplete: Boolean? = null
    ): VehicleInformationResult =
        mode09Result(
            pid = 0x06,
            payload = payload,
            responder = responder,
            messageCount = messageCount,
            multiFrameComplete = multiFrameComplete,
            decoded = Mode09Decoded.Cvn(decodeCvn(payload))
        )

    /** §7 P0.7: typed Mode 09 PID 090A (ECU name) — textual ASCII identifier. */
    fun decodeEcuNameResult(
        payload: ByteArray,
        responder: String? = null,
        messageCount: Int? = null,
        multiFrameComplete: Boolean? = null
    ): VehicleInformationResult =
        mode09Result(
            pid = 0x0A,
            payload = payload,
            responder = responder,
            messageCount = messageCount,
            multiFrameComplete = multiFrameComplete,
            decoded = Mode09Decoded.EcuName(decodeAsciiString(payload))
        )

    // -------------------------------------------------------------
    // §9 NEGATIVE RESPONSES (7F <service> <NRC>)
    // -------------------------------------------------------------

    /**
     * §9: true if [bytes] is an OBD-II negative response frame (service-id echo 0x7F).
     * A negative response is an explicit ECU rejection — it must never be parsed as
     * a positive payload, an empty result, or a fabricated DTC.
     */
    fun isNegativeResponse(bytes: ByteArray): Boolean =
        bytes.isNotEmpty() && (bytes[0].toInt() and 0xFF) == 0x7F

    /**
     * §9: parse "7F <requested-service> <NRC>" into a structured [NegativeResponse].
     * Unknown NRCs are retained numerically via nrcCode; [NegativeResponse.nrc] is null
     * only when the NRC is unrecognized.
     */
    fun parseNegativeResponse(
        bytes: ByteArray,
        responder: String? = null,
        rawLine: String = ""
    ): NegativeResponse? {
        if (bytes.size < 3) return null
        if ((bytes[0].toInt() and 0xFF) != 0x7F) return null
        val requestedService = bytes[1].toInt() and 0xFF
        val nrcCode = bytes[2].toInt() and 0xFF
        return NegativeResponse(
            requestedService = requestedService,
            nrc = ObdNrc.fromCode(nrcCode),
            nrcCode = nrcCode,
            rawLine = rawLine,
            responder = responder
        )
    }

    // -------------------------------------------------------------
    // §10 MODE 09 VEHICLE INFORMATION — support discovery
    // -------------------------------------------------------------

    /**
     * §10: Mode 09 PID-00 (0900) supported-PID bitmap for PIDs 0901..0920.
     * Reuses the standard 32-bit OBD bitmap math; the Mode 09 high-nibble context
     * (VIN / CALID / CVN / ECU name) is supplied by the caller on the command side.
     */
    fun decodeSupportedMode09Pids(bytes: ByteArray): Set<Int> {
        if (bytes.size < 4) return emptySet()
        return decodeSupportedPids(bytes, 0x00)
    }
}
