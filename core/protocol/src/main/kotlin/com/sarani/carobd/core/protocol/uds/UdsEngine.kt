package com.sarani.carobd.core.protocol.uds

/**
 * ISO 14229-1 (UDS) Service Identifiers.
 */
object UdsServiceId {
    const val DIAGNOSTIC_SESSION_CONTROL = 0x10
    const val ECU_RESET = 0x11
    const val CLEAR_DIAGNOSTIC_INFORMATION = 0x14
    const val READ_DTC_INFORMATION = 0x19
    const val READ_DATA_BY_IDENTIFIER = 0x22
    const val READ_MEMORY_BY_ADDRESS = 0x23
    const val SECURITY_ACCESS = 0x27
    const val WRITE_DATA_BY_IDENTIFIER = 0x2E
    const val INPUT_OUTPUT_CONTROL_BY_ID = 0x2F
    const val ROUTINE_CONTROL = 0x31
    const val TESTER_PRESENT = 0x3E
    const val CONTROL_DTC_SETTING = 0x85
    const val NEGATIVE_RESPONSE = 0x7F
}

/**
 * Standard UDS Data Identifiers (DIDs).
 */
object UdsStandardDid {
    const val VIN_DATA_IDENTIFIER = 0xF190
    const val SPARE_PART_NUMBER = 0xF187
    const val ECU_SOFTWARE_NUMBER = 0xF189
    const val ECU_MANUFACTURING_DATE = 0xF18B
    const val ECU_INSTALLATION_DATE = 0xF195
    const val SYSTEM_NAME_OR_ENGINE_TYPE = 0xF197
    const val REPAIR_SHOP_CODE_OR_SERIAL = 0xF198
    const val PROGRAMMING_DATE = 0xF199
    const val ACTIVE_DIAGNOSTIC_SESSION = 0xF186
}

/**
 * ISO 14229-1 Negative Response Codes (NRC).
 */
enum class UdsNrc(val code: Int, val descriptionEn: String, val descriptionFa: String) {
    GENERAL_REJECT(0x10, "General reject", "رد عمومی درخواست"),
    SERVICE_NOT_SUPPORTED(0x11, "Service not supported", "سرویس پشتیبانی نمی‌شود"),
    SUB_FUNCTION_NOT_SUPPORTED(0x12, "Sub-function not supported", "زیرتابع پشتیبانی نمی‌شود"),
    INCORRECT_MESSAGE_LENGTH(0x13, "Incorrect message length or invalid format", "طول پیام نادرست یا فرمت نامعتبر است"),
    RESPONSE_TOO_LONG(0x14, "Response too long", "طول پاسخ بیش از حد مجاز است"),
    BUSY_REPEAT_REQUEST(0x21, "Busy - repeat request", "یونیت کنترل مشغول است"),
    CONDITIONS_NOT_CORRECT(0x22, "Conditions not correct", "شرایط مجاز نیست (موتور باید خاموش باشد)"),
    REQUEST_SEQUENCE_ERROR(0x24, "Request sequence error", "توالی درخواست‌ها نامعتبر است"),
    REQUEST_OUT_OF_RANGE(0x31, "Request out of range", "پارامتر یا شناسه خارج از محدوده است"),
    SECURITY_ACCESS_DENIED(0x33, "Security access denied", "دسترسی امنیتی رد شد"),
    INVALID_KEY(0x35, "Invalid key", "کلید امنیتی اشتباه است"),
    EXCEED_NUMBER_OF_ATTEMPTS(0x36, "Exceed number of attempts", "تعداد دفعات تلاش بیش از حد مجاز"),
    REQUIRED_TIME_DELAY_NOT_EXPIRED(0x37, "Required time delay not expired", "زمان تاخیر امنیتی تمام نشده است"),
    RESPONSE_PENDING(0x78, "Request correctly received - response pending", "دستور دریافت شد - در حال پردازش");

    companion object {
        fun fromCode(code: Int): UdsNrc? = entries.firstOrNull { it.code == code }
    }
}

/**
 * UDS Safe Read-Only Client.
 * Builds requests and parses ISO 14229 responses safely.
 */
class UdsEngine {

    /**
     * Builds ReadDataByIdentifier (0x22) payload for a given 16-bit DID.
     */
    fun buildReadDidPayload(did: Int): ByteArray {
        return byteArrayOf(
            UdsServiceId.READ_DATA_BY_IDENTIFIER.toByte(),
            ((did ushr 8) and 0xFF).toByte(),
            (did and 0xFF).toByte()
        )
    }

    /**
     * Builds ReadDTCInformation (0x19) sub-function 0x02 (reportDTCByStatusMask).
     * @param statusMask e.g. 0x08 for confirmed DTCs, 0xFF for all statuses.
     */
    fun buildReadDtcByStatusPayload(statusMask: Int = 0x08): ByteArray {
        return byteArrayOf(
            UdsServiceId.READ_DTC_INFORMATION.toByte(),
            0x02.toByte(), // sub-function reportDTCByStatusMask
            (statusMask and 0xFF).toByte()
        )
    }

    /**
     * Builds DiagnosticSessionControl (0x10) payload.
     * 0x01: Default Session, 0x03: Extended Diagnostic Session
     */
    fun buildSessionControlPayload(sessionType: Int = 0x01): ByteArray {
        return byteArrayOf(
            UdsServiceId.DIAGNOSTIC_SESSION_CONTROL.toByte(),
            (sessionType and 0xFF).toByte()
        )
    }

    /**
     * Builds TesterPresent (0x3E) payload.
     * @param suppressPosRsp If true, sends 0x80 (no positive response required, saves bus bandwidth)
     */
    fun buildTesterPresentPayload(suppressPosRsp: Boolean = true): ByteArray {
        return byteArrayOf(
            UdsServiceId.TESTER_PRESENT.toByte(),
            if (suppressPosRsp) 0x80.toByte() else 0x00.toByte()
        )
    }

    /**
     * Builds ClearDiagnosticInformation (0x14) payload.
     * @param groupOfDTC 3-byte mask (0xFFFFFF clears all emission and non-emission DTCs)
     */
    fun buildClearDtcPayload(groupOfDTC: Int = 0xFFFFFF): ByteArray {
        return byteArrayOf(
            UdsServiceId.CLEAR_DIAGNOSTIC_INFORMATION.toByte(),
            ((groupOfDTC ushr 16) and 0xFF).toByte(),
            ((groupOfDTC ushr 8) and 0xFF).toByte(),
            (groupOfDTC and 0xFF).toByte()
        )
    }

    /**
     * Parses a raw UDS diagnostic response payload.
     */
    fun parseResponse(payload: ByteArray): UdsResponse {
        if (payload.isEmpty()) return UdsResponse.Error("Empty payload")

        val sid = payload[0].toInt() and 0xFF

        // Check for Negative Response (0x7F)
        if (sid == UdsServiceId.NEGATIVE_RESPONSE) {
            if (payload.size < 3) return UdsResponse.Error("Malformed negative response")
            val rejectedSid = payload[1].toInt() and 0xFF
            val nrcCode = payload[2].toInt() and 0xFF
            return UdsResponse.Negative(rejectedSid, nrcCode, UdsNrc.fromCode(nrcCode))
        }

        val data = if (payload.size > 1) payload.copyOfRange(1, payload.size) else ByteArray(0)
        return UdsResponse.Positive(sid, data)
    }

    /**
     * Parses the payload returned from ReadDataByIdentifier (SID echo 0x62).
     */
    fun parseReadDidResponse(payload: ByteArray): DidData? {
        // Expected: [0x62] [DID_High] [DID_Low] [Value...]
        if (payload.size < 3) return null
        val did = ((payload[1].toInt() and 0xFF) shl 8) or (payload[2].toInt() and 0xFF)
        val valueBytes = payload.copyOfRange(3, payload.size)
        return DidData(did, valueBytes)
    }
}

data class DidData(val did: Int, val rawBytes: ByteArray) {
    fun toAscii(): String = String(rawBytes).trim { it <= ' ' || it.code > 126 }
    val hexString: String get() = rawBytes.joinToString("") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DidData
        return did == other.did && rawBytes.contentEquals(other.rawBytes)
    }
    override fun hashCode(): Int = 31 * did + rawBytes.contentHashCode()
}

sealed class UdsResponse {
    data class Positive(val serviceId: Int, val data: ByteArray) : UdsResponse() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Positive
            return serviceId == other.serviceId && data.contentEquals(other.data)
        }
        override fun hashCode(): Int = 31 * serviceId + data.contentHashCode()
    }
    data class Negative(val rejectedServiceId: Int, val nrcCode: Int, val nrc: UdsNrc?) : UdsResponse()
    data class Error(val message: String) : UdsResponse()
}
