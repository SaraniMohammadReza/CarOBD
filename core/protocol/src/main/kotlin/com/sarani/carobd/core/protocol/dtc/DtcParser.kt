package com.sarani.carobd.core.protocol.dtc

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.DtcCategory
import com.sarani.carobd.core.model.DtcSeverity
import com.sarani.carobd.core.model.DtcSystem

/**
 * Robust Diagnostic Trouble Code (DTC) parser for:
 * 1. SAE J1979 2-byte DTCs (OBD-II Modes 03, 07, 0A)
 * 2. ISO 14229 / UDS 3-byte DTCs (with ISO/SAE Failure Mode Byte)
 * 3. SAE J1939 Heavy-Duty DTCs (SPN + FMI + OC)
 */
object DtcParser {

    /**
     * Parses a 2-byte standard OBD-II DTC pair (e.g. 0x01, 0x33 -> "P0133").
     */
    fun parseStandardObd2Dtc(
        byte1: Byte,
        byte2: Byte,
        category: DtcCategory = DtcCategory.STORED,
        responderAddressHex: String = "7E8"
    ): DiagnosticCode? {
        val b1 = byte1.toInt() and 0xFF
        val b2 = byte2.toInt() and 0xFF

        // 0000 0000 means empty padding in OBD-II response
        if (b1 == 0 && b2 == 0) return null

        val systemChar = when ((b1 ushr 6) and 0x03) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            3 -> 'U'
            else -> 'P'
        }

        val digit1 = (b1 ushr 4) and 0x03
        val digit2 = b1 and 0x0F
        val formattedCode = String.format("%c%d%01X%02X", systemChar, digit1, digit2, b2)

        val system = when (systemChar) {
            'P' -> DtcSystem.POWERTRAIN
            'C' -> DtcSystem.CHASSIS
            'B' -> DtcSystem.BODY
            'U' -> DtcSystem.NETWORK
            else -> DtcSystem.POWERTRAIN
        }

        val lookup = DtcLookupEngine.find(formattedCode)

        return DiagnosticCode(
            code = formattedCode,
            titleEn = lookup?.titleEn ?: "Diagnostic Trouble Code ($formattedCode)",
            titleFa = lookup?.titleFa ?: "کد خطای دیاگ ($formattedCode)",
            descriptionEn = lookup?.descriptionEn ?: "Unknown Diagnostic Trouble Code ($formattedCode)",
            descriptionFa = lookup?.descriptionFa ?: "کد خطای ناشناخته ($formattedCode)",
            severity = lookup?.severity ?: DtcSeverity.MODERATE,
            system = system,
            category = category,
            likelyCausesFa = lookup?.likelyCausesFa ?: emptyList(),
            diagnosticStepsFa = lookup?.diagnosticStepsFa ?: emptyList(),
            ecuSource = ecuNameFromAddress(responderAddressHex),
            responderAddressHex = responderAddressHex,
            freezeFrame = null,
            confidence = ConfidenceLevel.VERIFIED_STANDARD
        )
    }

    /**
     * Parses Mode 03 / 07 / 0A DTCs from a positively-acknowledged response that
     * STILL contains its service-echo byte (as produced by
     * [PidCodec.cleanResponseHex] on a raw ELM line such as "43 02 01 15 03 00").
     *
     * §4.3 framing contract — NO parity heuristic:
     *  - byte[0] MUST be the positive service echo matching [category]:
     *    STORED->0x43, PENDING->0x47, PERMANENT->0x4A.
     *  - byte[1] is the J1979 DTC *count*.
     *  - DTC pairs follow; trailing 00 00 padding is ignored.
     *
     * §9: a 7F negative-response frame yields NO DTCs (never fabricated). A
     * missing/mismatched echo is treated as malformed framing and yields nothing;
     * [DtcScanValidator] surfaces any explicit 7F rejection to the caller.
     *
     * @param responderAddressHex responder id (e.g. "7E8"/"7E9"); an unknown id is
     *        labelled "ECU <id>" downstream — never guessed as ABS/SRS/BCM.
     */
    fun parseMode03Payload(
        payload: ByteArray,
        category: DtcCategory = DtcCategory.STORED,
        responderAddressHex: String = "7E8"
    ): List<DiagnosticCode> {
        val dtcs = mutableListOf<DiagnosticCode>()
        if (payload.isEmpty()) return dtcs
        val first = payload[0].toInt() and 0xFF
        // §9: a 0x7F rejection must never be mis-parsed into fabricated DTCs.
        if (first == 0x7F) return dtcs
        // §4.3: validate positive service echo explicitly — never trust length parity.
        if (first != mode0xEcho(category)) return dtcs
        if (payload.size < 2) return dtcs
        val count = payload[1].toInt() and 0xFF
        var offset = 2
        while (offset + 1 < payload.size && dtcs.size < count) {
            val dtc = parseStandardObd2Dtc(payload[offset], payload[offset + 1], category, responderAddressHex)
            if (dtc != null) {
                dtcs.add(dtc)
            }
            offset += 2
        }
        return dtcs
    }

    /** J1979 positive-response service echo byte for each DTC category. */
    private fun mode0xEcho(category: DtcCategory): Int = when (category) {
        DtcCategory.STORED -> 0x43      // Mode 03 response
        DtcCategory.PENDING -> 0x47     // Mode 07 response
        DtcCategory.PERMANENT -> 0x4A   // Mode 0A response
        DtcCategory.HISTORY -> 0x43     // not a standard OBD-II echo; degrade safely
    }

    /**
     * Parses ISO 14229 UDS DTC (3 bytes: High byte, Low byte, Failure Type Byte).
     */
    fun parseUdsDtc(
        highByte: Byte,
        lowByte: Byte,
        failureTypeByte: Byte,
        statusMask: Int = 0,
        responderAddressHex: String = "7E8"
    ): DiagnosticCode? {
        val b1 = highByte.toInt() and 0xFF
        val b2 = lowByte.toInt() and 0xFF
        val ftb = failureTypeByte.toInt() and 0xFF

        if (b1 == 0 && b2 == 0) return null

        val systemChar = when ((b1 ushr 6) and 0x03) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            3 -> 'U'
            else -> 'P'
        }

        val digit1 = (b1 ushr 4) and 0x03
        val digit2 = b1 and 0x0F
        val baseCode = String.format("%c%d%01X%02X", systemChar, digit1, digit2, b2)
        val fullCode = String.format("%s-%02X", baseCode, ftb)

        val ftbDesc = decodeFailureTypeByte(ftb)
        val lookup = DtcLookupEngine.find(baseCode)

        val category = when {
            (statusMask and 0x08) != 0 -> DtcCategory.STORED // confirmedDTC
            (statusMask and 0x04) != 0 -> DtcCategory.PENDING // pendingDTC
            else -> DtcCategory.HISTORY
        }

        return DiagnosticCode(
            code = fullCode,
            titleEn = (lookup?.titleEn ?: baseCode) + " [FTB 0x%02X]".format(ftb),
            titleFa = (lookup?.titleFa ?: baseCode) + " [نقص 0x%02X]".format(ftb),
            descriptionEn = (lookup?.descriptionEn ?: baseCode) + " [" + ftbDesc.en + "]",
            descriptionFa = (lookup?.descriptionFa ?: baseCode) + " [" + ftbDesc.fa + "]",
            severity = lookup?.severity ?: DtcSeverity.MODERATE,
            system = when (systemChar) {
                'P' -> DtcSystem.POWERTRAIN
                'C' -> DtcSystem.CHASSIS
                'B' -> DtcSystem.BODY
                'U' -> DtcSystem.NETWORK
                else -> DtcSystem.POWERTRAIN
            },
            category = category,
            likelyCausesFa = lookup?.likelyCausesFa ?: emptyList(),
            diagnosticStepsFa = lookup?.diagnosticStepsFa ?: emptyList(),
            ecuSource = "UDS Server",
            responderAddressHex = responderAddressHex,
            freezeFrame = null,
            confidence = ConfidenceLevel.VERIFIED_STANDARD
        )
    }

    /**
     * Decodes standard ISO 14229-1 Annex D Failure Type Byte (FTB).
     */
    private fun decodeFailureTypeByte(ftb: Int): FtbDescription {
        return when (ftb) {
            0x11 -> FtbDescription("Circuit short to ground", "اتصال کوتاه مدار به بدنه/زمین")
            0x12 -> FtbDescription("Circuit short to battery", "اتصال کوتاه مدار به مثبت باتری")
            0x13 -> FtbDescription("Circuit open", "قطعی یا باز بودن مدار")
            0x14 -> FtbDescription("Circuit resistance below threshold", "مقاومت مدار کمتر از حد مجاز")
            0x15 -> FtbDescription("Circuit resistance above threshold", "مقاومت مدار بیشتر از حد مجاز")
            0x16 -> FtbDescription("Circuit voltage below threshold", "ولتاژ مدار پایین‌تر از آستانه")
            0x17 -> FtbDescription("Circuit voltage above threshold", "ولتاژ مدار بالاتر از آستانه")
            0x21 -> FtbDescription("Signal amplitude < minimum", "دامنه سیگنال کمتر از حداقل")
            0x22 -> FtbDescription("Signal amplitude > maximum", "دامنه سیگنال بیشتر از حداکثر")
            0x29 -> FtbDescription("Signal invalid", "سیگنال نامعتبر")
            0x71 -> FtbDescription("Actuator stuck", "عملگر گیر کرده است")
            0x72 -> FtbDescription("Actuator stuck open", "عملگر در حالت باز گیر کرده است")
            0x73 -> FtbDescription("Actuator stuck closed", "عملگر در حالت بسته گیر کرده است")
            0x81 -> FtbDescription("Invalid serial data received", "داده سریال نامعتبر دریافت شد")
            0x87 -> FtbDescription("Missing message", "عدم دریافت پیام در شبکه")
            else -> FtbDescription("General failure (0x%02X)".format(ftb), "خطای عمومی (0x%02X)".format(ftb))
        }
    }

    /**
     * §13: preserve responder identity instead of assuming a single engine ECU.
     * Only standard OBD-II engine addresses are named; every other responder ID
     * is kept neutral ("ECU <id>") — never ABS/SRS/BCM manufacturer claims from a
     * generic OBD responder.
     */
    private fun ecuNameFromAddress(hex: String): String = when (hex.trim().uppercase()) {
        "7E8", "7E9", "7E0", "7E1" -> "Engine ECU"
        else -> "ECU $hex"
    }

    private data class FtbDescription(val en: String, val fa: String)
}
