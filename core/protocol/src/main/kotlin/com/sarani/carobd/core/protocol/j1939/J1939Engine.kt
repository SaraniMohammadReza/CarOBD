package com.sarani.carobd.core.protocol.j1939

/**
 * SAE J1939 29-bit CAN Identifier Decoder.
 */
data class J1939CanId(
    val rawId: Long,
    val priority: Int,
    val isPdu1Format: Boolean,
    val pgn: Int,
    val destinationAddress: Int?,
    val sourceAddress: Int
) {
    companion object {
        fun parse(canId: Long): J1939CanId {
            val priority = ((canId ushr 26) and 0x07).toInt()
            val edp = ((canId ushr 25) and 0x01).toInt()
            val dp = ((canId ushr 24) and 0x01).toInt()
            val pf = ((canId ushr 16) and 0xFF).toInt()
            val ps = ((canId ushr 8) and 0xFF).toInt()
            val sa = (canId and 0xFF).toInt()

            val isPdu1 = pf < 240
            val pgn = if (isPdu1) {
                // PDU1: PGN = (DP << 16) | (PF << 8)
                (dp shl 16) or (pf shl 8)
            } else {
                // PDU2 broadcast: PGN = (DP << 16) | (PF << 8) | PS
                (dp shl 16) or (pf shl 8) or ps
            }

            val da = if (isPdu1) ps else null

            return J1939CanId(
                rawId = canId,
                priority = priority,
                isPdu1Format = isPdu1,
                pgn = pgn,
                destinationAddress = da,
                sourceAddress = sa
            )
        }
    }
}

/**
 * SAE J1939 Diagnostic Trouble Code (SPN + FMI + OC).
 */
data class J1939Dtc(
    val spn: Int,
    val fmi: Int,
    val occurrenceCount: Int,
    val spnDescription: String,
    val fmiDescriptionEn: String,
    val fmiDescriptionFa: String
)

/**
 * Lamp status from DM1/DM2 messages.
 */
data class J1939LampStatus(
    val milLamp: Boolean,
    val redStopLamp: Boolean,
    val amberWarningLamp: Boolean,
    val protectLamp: Boolean
)

/**
 * J1939 Heavy Duty Diagnostic Parser.
 */
object J1939Engine {

    const val PGN_DM1_ACTIVE_DTCS = 65226 // 0x00FECA
    const val PGN_DM2_PREVIOUS_DTCS = 65227 // 0x00FECB
    const val PGN_DM11_CLEAR_ACTIVE_DTCS = 65235 // 0x00FED3

    /**
     * Parses standard DM1 or DM2 payload.
     */
    fun parseDmMessage(payload: ByteArray): Pair<J1939LampStatus, List<J1939Dtc>> {
        if (payload.size < 2) {
            return Pair(J1939LampStatus(false, false, false, false), emptyList())
        }

        // Byte 1: Malfunction Indicator Lamp (bits 8-7), Red Stop Lamp (bits 6-5), Amber Warning Lamp (bits 4-3), Protect Lamp (bits 2-1)
        val b1 = payload[0].toInt() and 0xFF
        val lampStatus = J1939LampStatus(
            milLamp = ((b1 ushr 6) and 0x03) == 0x01,
            redStopLamp = ((b1 ushr 4) and 0x03) == 0x01,
            amberWarningLamp = ((b1 ushr 2) and 0x03) == 0x01,
            protectLamp = (b1 and 0x03) == 0x01
        )

        val dtcList = mutableListOf<J1939Dtc>()
        var offset = 2 // Skip 2 lamp bytes

        while (offset + 4 <= payload.size) {
            val byte1 = payload[offset].toInt() and 0xFF
            val byte2 = payload[offset + 1].toInt() and 0xFF
            val byte3 = payload[offset + 2].toInt() and 0xFF
            val byte4 = payload[offset + 3].toInt() and 0xFF

            // SPN: Byte 1 (bits 1-8) | Byte 2 (bits 9-16) | (Byte 3 bits 6-8 shifted 11)
            val spn = byte1 or (byte2 shl 8) or ((byte3 and 0xE0) shl 11)
            val fmi = byte3 and 0x1F
            val occurrenceCount = byte4 and 0x7F

            // SPN 0 indicates no DTC
            if (spn > 0) {
                val fmiInfo = getFmiDescription(fmi)
                dtcList.add(
                    J1939Dtc(
                        spn = spn,
                        fmi = fmi,
                        occurrenceCount = occurrenceCount,
                        spnDescription = getKnownSpnDescription(spn),
                        fmiDescriptionEn = fmiInfo.first,
                        fmiDescriptionFa = fmiInfo.second
                    )
                )
            }
            offset += 4
        }

        return Pair(lampStatus, dtcList)
    }

    /**
     * Standard SAE J1939 Failure Mode Identifiers (FMI).
     */
    fun getFmiDescription(fmi: Int): Pair<String, String> {
        return when (fmi) {
            0 -> Pair("Data Valid But Above Normal Operational Range (Most Severe)", "داده معتبر اما بالاتر از حد مجاز کاری (شدید)")
            1 -> Pair("Data Valid But Below Normal Operational Range (Most Severe)", "داده معتبر اما پایین‌تر از حد مجاز کاری (شدید)")
            2 -> Pair("Data Erratic, Intermittent Or Incorrect", "داده نامنظم، متناوب یا نادرست")
            3 -> Pair("Voltage Above Normal, Or Shorted To High Source", "ولتاژ بالاتر از حد نرمال، یا اتصال به مثبت")
            4 -> Pair("Voltage Below Normal, Or Shorted To Low Source", "ولتاژ پایین‌تر از حد نرمال، یا اتصال به بدنه")
            5 -> Pair("Current Below Normal Or Open Circuit", "جریان پایین‌تر از حد نرمال یا مدار باز/قطعی")
            6 -> Pair("Current Above Normal Or Grounded Circuit", "جریان بالاتر از حد نرمال یا اتصال کوتاه")
            7 -> Pair("Mechanical System Not Responding Or Out Of Adjustment", "سیستم مکانیکی پاسخ نمی‌دهد یا تنظیم نیست")
            8 -> Pair("Abnormal Frequency Or Pulse Width Or Period", "فرکانس، پهنای پالس یا دوره تناوب غیرعادی")
            9 -> Pair("Abnormal Update Rate", "نرخ بروزرسانی غیرعادی در شبکه")
            10 -> Pair("Abnormal Rate Of Change", "نرخ تغییرات غیرعادی و ناگهانی")
            11 -> Pair("Root Cause Not Known", "علت ریشه‌ای خطا نامشخص")
            12 -> Pair("Bad Intelligent Device Or Component", "خرابی واحد پردازنده هوشمند")
            13 -> Pair("Out Of Calibration", "خارج از کالیبراسیون")
            14 -> Pair("Special Instructions", "دستورالعمل‌های ویژه عیب‌یابی")
            15 -> Pair("Data Valid But Above Normal (Least Severe)", "داده بالاتر از حد مجاز (خفیف)")
            16 -> Pair("Data Valid But Above Normal (Moderately Severe)", "داده بالاتر از حد مجاز (متوسط)")
            17 -> Pair("Data Valid But Below Normal (Least Severe)", "داده پایین‌تر از حد مجاز (خفیف)")
            18 -> Pair("Data Valid But Below Normal (Moderately Severe)", "داده پایین‌تر از حد مجاز (متوسط)")
            19 -> Pair("Received Network Data In Error", "داده خطادار دریافت شده از شبکه")
            31 -> Pair("Condition Exists", "شرایط خطا برقرار است")
            else -> Pair("Reserved / Unknown FMI ($fmi)", "کد نقص رزرو شده ($fmi)")
        }
    }

    private fun getKnownSpnDescription(spn: Int): String {
        return when (spn) {
            84 -> "Wheel-Based Vehicle Speed"
            91 -> "Accelerator Pedal Position 1"
            94 -> "Fuel Delivery Pressure"
            100 -> "Engine Oil Pressure"
            102 -> "Engine Intake Manifold #1 Pressure (Boost)"
            105 -> "Engine Intake Manifold 1 Temperature"
            108 -> "Barometric Pressure"
            110 -> "Engine Coolant Temperature"
            158 -> "Key Switch Battery Potential (Voltage)"
            168 -> "Battery Potential / Power Input 1"
            174 -> "Fuel Temperature"
            175 -> "Engine Oil Temperature 1"
            190 -> "Engine Speed (RPM)"
            512 -> "Driver's Demand Engine - Percent Torque"
            513 -> "Actual Engine - Percent Torque"
            1110 -> "Engine Protection System Has Shutdown Engine"
            3216 -> "Aftertreatment 1 Intake NOx"
            3226 -> "Aftertreatment 1 Outlet NOx"
            3251 -> "Aftertreatment 1 Diesel Particulate Filter Differential Pressure"
            else -> "Suspect Parameter Number $spn"
        }
    }
}
