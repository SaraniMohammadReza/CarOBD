package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.model.NotConnectedException

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
            is AdapterResponse.Success -> null
            is AdapterResponse.Error -> NotConnectedException(mode03Response.error.messageFa)
            null -> NotConnectedException("اتصال به کامپیوتر خودرو برقرار نشده است")
        }
}
