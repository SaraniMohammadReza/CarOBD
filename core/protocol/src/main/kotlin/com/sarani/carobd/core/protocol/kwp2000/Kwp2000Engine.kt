package com.sarani.carobd.core.protocol.kwp2000

/**
 * ISO 14230 (KWP2000) Service Identifiers.
 */
object KwpServiceId {
    const val START_COMMUNICATION = 0x81
    const val STOP_COMMUNICATION = 0x82
    const val ACCESS_TIMING_PARAMETERS = 0x83
    const val START_DIAGNOSTIC_SESSION = 0x10
    const val STOP_DIAGNOSTIC_SESSION = 0x20
    const val READ_STATUS_OF_DTC = 0x17
    const val READ_DTC_BY_STATUS = 0x18
    const val READ_ECU_IDENTIFICATION = 0x1A
    const val CLEAR_DIAGNOSTIC_INFORMATION = 0x14
    const val READ_DATA_BY_LOCAL_IDENTIFIER = 0x21
    const val READ_DATA_BY_COMMON_IDENTIFIER = 0x22
    const val READ_MEMORY_BY_ADDRESS = 0x23
    const val WRITE_DATA_BY_LOCAL_IDENTIFIER = 0x3B
    const val INPUT_OUTPUT_CONTROL_BY_LOCAL_ID = 0x30
    const val TESTER_PRESENT = 0x3E
    const val NEGATIVE_RESPONSE = 0x7F
}

/**
 * Standard KWP2000 Diagnostic Session Modes.
 */
enum class KwpSessionType(val code: Int) {
    STANDARD_DIAGNOSTIC(0x81),
    ECU_PROGRAMMING(0x85),
    ECU_ADJUSTMENT(0x86),
    STANDBY(0xC0)
}

/**
 * KWP2000 Negative Response Codes (NRC).
 */
enum class KwpNrc(val code: Int, val descriptionEn: String, val descriptionFa: String) {
    GENERAL_REJECT(0x10, "General reject", "رد درخواست عمومی"),
    SERVICE_NOT_SUPPORTED(0x11, "Service not supported", "سرویس در این ECU پشتیبانی نمی‌شود"),
    SUB_FUNCTION_NOT_SUPPORTED(0x12, "Sub-function not supported", "زیرتابع پشتیبانی نمی‌شود"),
    BUSY_REPEAT_REQUEST(0x21, "Busy - repeat request", "کنترل یونیت مشغول است، تکرار کنید"),
    CONDITIONS_NOT_CORRECT(0x22, "Conditions not correct (e.g. engine running)", "شرایط اجرای دستور مهیا نیست (مثلا موتور روشن است)"),
    REQUEST_SEQUENCE_ERROR(0x24, "Request sequence error", "خطای توالی اجرای دستورات"),
    REQUEST_OUT_OF_RANGE(0x31, "Request out of range", "پارامتر خارج از محدوده مجاز"),
    SECURITY_ACCESS_DENIED(0x33, "Security access denied", "دسترسی امنیتی رد شد (نیاز به بازگشایی قفل)"),
    INVALID_KEY(0x35, "Invalid key", "کلید امنیتی نامعتبر"),
    EXCEEDED_NUMBER_OF_ATTEMPTS(0x36, "Exceeded number of attempts", "تعداد دفعات ورود اشتباه بیش از حد مجاز"),
    REQUIRED_TIME_DELAY_NOT_EXPIRED(0x37, "Required time delay not expired", "زمان تاخیر امنیتی سپری نشده است"),
    RESPONSE_PENDING(0x78, "Request correctly received - response pending", "دستور دریافت شد - در حال پردازش در ECU");

    companion object {
        fun fromCode(code: Int): KwpNrc? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Complete KWP2000 Packet Builder and Parser.
 */
class Kwp2000Engine(
    private val targetAddress: Int = 0x10, // Default ECU Target (e.g. 0x10 for engine)
    private val sourceAddress: Int = 0xF1  // Default Scan Tool Source
) {

    /**
     * Builds a full KWP2000 frame including Header, Target, Source, Length, SID, Data, and Checksum.
     */
    fun buildFrame(serviceId: Int, data: ByteArray = ByteArray(0)): ByteArray {
        val payloadLen = 1 + data.size // SID + Data
        val hasLengthByte = payloadLen > 63

        val fmtByte = if (hasLengthByte) {
            0x80 // Bit 7=1 (physical addressing), Length=0 in format byte, followed by length byte
        } else {
            0x80 or (payloadLen and 0x3F)
        }

        val frameList = mutableListOf<Byte>()
        frameList.add(fmtByte.toByte())
        frameList.add(targetAddress.toByte())
        frameList.add(sourceAddress.toByte())

        if (hasLengthByte) {
            frameList.add(payloadLen.toByte())
        }

        frameList.add(serviceId.toByte())
        for (b in data) {
            frameList.add(b)
        }

        // Calculate checksum: Sum of all bytes modulo 256
        var checksum = 0
        for (b in frameList) {
            checksum += (b.toInt() and 0xFF)
        }
        frameList.add((checksum and 0xFF).toByte())

        return frameList.toByteArray()
    }

    /**
     * Verifies and extracts the payload from a received KWP2000 raw byte frame.
     */
    fun parseFrame(frame: ByteArray): KwpResponse {
        if (frame.size < 4) {
            return KwpResponse.Error("Frame too short (${frame.size} bytes)")
        }

        // Verify checksum
        var calculatedSum = 0
        for (i in 0 until frame.size - 1) {
            calculatedSum += (frame[i].toInt() and 0xFF)
        }
        val expectedChecksum = calculatedSum and 0xFF
        val actualChecksum = frame.last().toInt() and 0xFF

        if (expectedChecksum != actualChecksum) {
            return KwpResponse.Error("Checksum mismatch: expected 0x%02X, got 0x%02X".format(expectedChecksum, actualChecksum))
        }

        val fmt = frame[0].toInt() and 0xFF
        var offset = 3 // fmt + tgt + src
        var dataLen = fmt and 0x3F

        if (dataLen == 0) {
            dataLen = frame[offset].toInt() and 0xFF
            offset++
        }

        if (offset >= frame.size - 1) {
            return KwpResponse.Error("Malformed header")
        }

        val sid = frame[offset].toInt() and 0xFF
        offset++

        // Check for Negative Response
        if (sid == KwpServiceId.NEGATIVE_RESPONSE) {
            val rejectedSid = if (offset < frame.size - 1) frame[offset].toInt() and 0xFF else 0
            val nrcCode = if (offset + 1 < frame.size - 1) frame[offset + 1].toInt() and 0xFF else 0
            val nrc = KwpNrc.fromCode(nrcCode)
            return KwpResponse.Negative(rejectedSid, nrcCode, nrc)
        }

        // Positive Response payload (everything after SID up to checksum)
        val payloadLenActual = (frame.size - 1) - offset
        val payload = ByteArray(payloadLenActual)
        System.arraycopy(frame, offset, payload, 0, payloadLenActual)

        return KwpResponse.Positive(sid, payload)
    }

    /**
     * Helper to create TesterPresent (0x3E) frame to maintain active KWP session.
     */
    fun createTesterPresentFrame(): ByteArray = buildFrame(KwpServiceId.TESTER_PRESENT)

    /**
     * Helper to create StartDiagnosticSession frame.
     */
    fun createStartSessionFrame(sessionType: KwpSessionType = KwpSessionType.STANDARD_DIAGNOSTIC): ByteArray =
        buildFrame(KwpServiceId.START_DIAGNOSTIC_SESSION, byteArrayOf(sessionType.code.toByte()))

    /**
     * Helper to read ECU Identification (0x1A) for specified record type.
     */
    fun createReadEcuIdFrame(idType: Int = 0x90): ByteArray =
        buildFrame(KwpServiceId.READ_ECU_IDENTIFICATION, byteArrayOf(idType.toByte()))

    /**
     * Helper to read DTCs by status mask (0x18).
     */
    fun createReadDtcsFrame(statusMask: Int = 0x02): ByteArray =
        buildFrame(KwpServiceId.READ_DTC_BY_STATUS, byteArrayOf(statusMask.toByte()))

    /**
     * Helper to clear diagnostic info (0x14).
     */
    fun createClearDtcsFrame(): ByteArray =
        buildFrame(KwpServiceId.CLEAR_DIAGNOSTIC_INFORMATION)

    /**
     * Helper to read Data by Local Identifier (0x21).
     */
    fun createReadLocalIdFrame(localId: Int): ByteArray =
        buildFrame(KwpServiceId.READ_DATA_BY_LOCAL_IDENTIFIER, byteArrayOf(localId.toByte()))
}

sealed class KwpResponse {
    data class Positive(val serviceId: Int, val payload: ByteArray) : KwpResponse() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Positive
            return serviceId == other.serviceId && payload.contentEquals(other.payload)
        }
        override fun hashCode(): Int = 31 * serviceId + payload.contentHashCode()
    }

    data class Negative(val rejectedServiceId: Int, val nrcCode: Int, val nrc: KwpNrc?) : KwpResponse()
    data class Error(val message: String) : KwpResponse()
}
