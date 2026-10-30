package com.sarani.carobd.core.model

data class DtcHistoryCode(val code: String, val category: DtcCategory?, val responder: String?, val mil: Boolean?,
    val protocol: String?, val observedAtMs: Long, val legacy: Boolean = false) {
    companion object {
        fun capture(code: DiagnosticCode, protocol: String?, mil: Boolean?, time: Long) =
            DtcHistoryCode(code.code, code.category, code.responderAddressHex, mil, protocol, time)
    }
}
data class DtcHistorySession(val id: String, val connectionSessionId: String?, val vehicleId: String?, val observedAtMs: Long,
    val protocol: String?, val mil: Boolean?, val codes: List<DtcHistoryCode>, val legacy: Boolean = false)
