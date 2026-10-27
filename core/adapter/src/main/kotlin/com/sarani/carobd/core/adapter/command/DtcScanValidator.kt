package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.model.NotConnectedException
import com.sarani.carobd.core.protocol.obd2.PidCodec

/**
 * Truth gate for DTC scanning (mirrors the §28 connection/diagnostics rules).
 *
 * Mode 03 (stored DTCs) is the canonical "ECU is answering" signal. A non-success
 * or absent Mode 03 response means the ECU is unreachable — which MUST surface as
 * [NotConnectedException] and NEVER as an empty "no codes found" result. Returning
 * success-with-empty on a dead ECU is exactly the "Adapter not connected" +
 * "No trouble codes found" simultaneous-display bug.
 *
 * Lives in [com.sarani.carobd.core.adapter.command] because it must reference
 * [AdapterResponse]/[AdapterError], and core/protocol does not depend on
 * core/adapter.
 */
object DtcScanValidator {

    /**
     * @return a [NotConnectedException] describing why the ECU is unreachable,
     *         or null when Mode 03 produced a valid (Success) response.
     */
    fun notConnectedError(mode03Response: AdapterResponse?): NotConnectedException? =
        when (mode03Response) {
            is AdapterResponse.Success -> {
                // §9: a 7F negative response is an explicit ECU rejection — NOT
                // "no trouble codes found." It must surface as a scan failure and
                // must never be parsed into fabricated DTCs downstream.
                val cleanHex = PidCodec.cleanResponseHex(mode03Response.rawResponse)
                if (PidCodec.isNegativeResponse(cleanHex)) {
                    val neg = PidCodec.parseNegativeResponse(cleanHex, rawLine = mode03Response.rawResponse)
                    val nrcDesc = neg?.nrc?.descriptionEn ?: "negative response"
                    NotConnectedException(
                        "ECU rejected Mode 03 request (NRC 0x%02X: %s)"
                            .format(neg?.nrcCode ?: 0, nrcDesc)
                    )
                } else null
            }
            is AdapterResponse.Error -> NotConnectedException(mode03Response.error.messageFa)
            null -> NotConnectedException("اتصال به کامپیوتر خودرو برقرار نشده است")
        }
}
