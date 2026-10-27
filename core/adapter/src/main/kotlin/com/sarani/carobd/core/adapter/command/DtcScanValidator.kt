package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.model.EcuRejectionException
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
     * @return an [Exception] describing why a DTC scan cannot proceed
     *         *truthfully*:
     *         - [NotConnectedException] when the ECU is unreachable (no adapter,
     *           transport error, or timeout — the ECU never answered).
     *         - [EcuRejectionException] when the ECU answered but explicitly
     *           NACK'd with a 7F negative response (it WAS reachable, the request
     *           was rejected by NRC).
     *         OR null when the response is a clean positive acknowledgement and
     *         the scan may proceed.
     *
     * §4.4: 7F is an ECU *response*, never "not connected" and never
     * "no codes found." Collapsing it into either is a fabrication.
     */
    fun scanFailure(response: AdapterResponse?): Exception? =
        when (response) {
            null -> NotConnectedException("آداپتور متصل نیست")
            is AdapterResponse.Error -> NotConnectedException(response.error.messageFa)
            is AdapterResponse.Success -> {
                // §9 / §4.4: a 7F is an explicit ECU rejection, NOT "no codes found".
                val cleanHex = PidCodec.cleanResponseHex(response.rawResponse)
                if (PidCodec.isNegativeResponse(cleanHex)) {
                    val neg = PidCodec.parseNegativeResponse(
                        cleanHex, responder = null, rawLine = response.rawResponse
                    )
                    if (neg != null) EcuRejectionException(neg)
                    else NotConnectedException("خطا دریافت پاسخ منفی 7F از ECU")
                } else {
                    null
                }
            }
        }
}
