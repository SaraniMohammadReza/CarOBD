package com.sarani.carobd.core.model

/**
 * §7 P0.7 — typed Mode 09 vehicle-information results.
 *
 * Every result preserves: PID, responder identity, raw bytes, decoded data,
 * message/index count, and multi-frame completion state. A value is typed only
 * when the J1979 standard unambiguously defines its encoding; otherwise it is
 * carried as raw/unknown (never fabricated).
 *
 * Calibration Verification Numbers (Mode 09 PID 0906) are a sequence of
 * 4-byte binary values — they are NOT generic ASCII text. Treating CVN as
 * ASCII is a truthfulness defect: it would re-label "F1234ABCD5" as a string
 * instead of recognising two distinct calibration IDs. Each CVN is preserved
 * as an 8-char hex string ("1234ABCD"). Trailing partial groups are dropped.
 */
data class CalibrationVerificationNumber(
    val values: List<String>
) {
    init {
        values.forEach { raw ->
            require(raw.length == 8) {
                "CVN must be exactly 4 bytes (8 hex chars): $raw"
            }
        }
    }

    val count: Int get() = values.size
}

/**
 * §7 P0.7: typed, source-honest decoding of a Mode 09 PID response.
 * Unknown/unimplemented PIDs surface as [Mode09Decoded.Unknown] carrying the
 * raw bytes — never as a fabricated label.
 */
sealed interface Mode09Decoded {
    /** Mode 09 PID 0902 (VIN) — 17-char ASCII VIN. */
    data class Vin(val value: String) : Mode09Decoded

    /** Mode 09 PID 0904 (Calibration ID / CALID) — textual ASCII identifier. */
    data class CalibrationId(val value: String) : Mode09Decoded

    /** Mode 09 PID 0906 (CVN) — typed 4-byte binary values (NOT ASCII). */
    data class Cvn(val cvn: CalibrationVerificationNumber) : Mode09Decoded

    /** Mode 09 PID 090A (ECU name) — textual ASCII identifier. */
    data class EcuName(val value: String) : Mode09Decoded

    /** Mode 09 PID not yet given a typed decoder — raw bytes carried, never guessed. */
    data class Unknown(val rawBytes: ByteArray) : Mode09Decoded {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Unknown && rawBytes.contentEquals(other.rawBytes))

        override fun hashCode(): Int = rawBytes.contentHashCode()
    }
}

/**
 * §7 P0.7: a fully-typed Mode 09 response envelope that cannot collapse an
 * ECU rejection into a positive value. `negativeResponse` is populated when the
 * ECU explicitly NACK'd the request (7F); it must never be confused with a
 * positive decode or with "no data".
 */
data class VehicleInformationResult(
    val pid: Int,
    val responder: String? = null,
    val rawBytes: ByteArray,
    val decoded: Mode09Decoded? = null,
    val messageCount: Int? = null,
    val multiFrameComplete: Boolean? = null,
    val negativeResponse: NegativeResponse? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VehicleInformationResult) return false
        return pid == other.pid &&
            responder == other.responder &&
            messageCount == other.messageCount &&
            multiFrameComplete == other.multiFrameComplete &&
            decoded == other.decoded &&
            negativeResponse == other.negativeResponse &&
            rawBytes.contentEquals(other.rawBytes)
    }

    override fun hashCode(): Int {
        var h = pid.hashCode()
        h = h * 31 + (responder?.hashCode() ?: 0)
        h = h * 31 + rawBytes.contentHashCode()
        h = h * 31 + (decoded?.hashCode() ?: 0)
        h = h * 31 + (messageCount ?: 0)
        h = h * 31 + (multiFrameComplete?.hashCode() ?: 0)
        h = h * 31 + (negativeResponse?.hashCode() ?: 0)
        return h
    }
}
