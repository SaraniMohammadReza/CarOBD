package com.sarani.carobd.core.model

/**
 * §4.4: an ECU that responded with a negative response (`7F <service> <NRC>`).
 *
 * This is deliberately distinct from [NotConnectedException]: the ECU was
 * reachable and answered, but explicitly rejected the request. Conflating the
 * two (or collapsing 7F into "no codes found") is a fabrication, because it
 * erases the difference between "ECU is silent" and "ECU said no."
 *
 * Consumers MUST surface this as an explicit ECU rejection carrying the NRC —
 * never as a successful empty scan, and never as a connection loss.
 */
class EcuRejectionException(
    val negativeResponse: NegativeResponse,
    message: String = buildMessage(negativeResponse)
) : Exception(message) {
    companion object {
        fun buildMessage(n: NegativeResponse): String {
            val nrcText = n.nrc?.descriptionEn ?: "unknown NRC 0x%02X".format(n.nrcCode)
            val service = "0x%02X".format(n.requestedService)
            val responder = if (!n.responder.isNullOrEmpty()) " from ${n.responder}" else ""
            return "ECU reachable but rejected request for $service$responder (NRC $nrcText)"
        }
    }
}
