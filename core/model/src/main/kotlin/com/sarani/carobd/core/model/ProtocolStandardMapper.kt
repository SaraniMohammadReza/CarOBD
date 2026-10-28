package com.sarani.carobd.core.model

/**
 * §18 MANUAL protocol code → [ProtocolStandard] mapping.
 *
 * Codes follow the ELM327 AT SP selector (0–9, A). "0" maps to [AUTO]. Codes the
 * backend does not model (e.g. B/C — only meaningful where a clone truly supports
 * them) return null so callers surface [ProtocolConfigurationFailed] rather than
 * silently auto-falling-back to AT SP 0.
 */
fun protocolFromManualCode(code: String): ProtocolStandard? = when (code.trim().uppercase()) {
    "0" -> ProtocolStandard.AUTO
    "1" -> ProtocolStandard.SAE_J1850_PWM
    "2" -> ProtocolStandard.SAE_J1850_VPW
    "3" -> ProtocolStandard.ISO_9141_2
    "4" -> ProtocolStandard.ISO_14230_4_KWP_5BAUD
    "5" -> ProtocolStandard.ISO_14230_4_KWP_FAST
    "6" -> ProtocolStandard.ISO_15765_4_CAN_11_500
    "7" -> ProtocolStandard.ISO_15765_4_CAN_29_500
    "8" -> ProtocolStandard.ISO_15765_4_CAN_11_250
    "9" -> ProtocolStandard.ISO_15765_4_CAN_29_250
    "A" -> ProtocolStandard.SAE_J1939_CAN
    else -> null
}
