package com.sarani.carobd.core.model

enum class ProtocolStandard(val displayName: String, val atCommand: String) {
    AUTO("Automatic Search", "AT SP 0"),
    ISO_15765_4_CAN_11_500("ISO 15765-4 CAN (11 bit ID, 500 kbaud)", "AT SP 6"),
    ISO_15765_4_CAN_29_500("ISO 15765-4 CAN (29 bit ID, 500 kbaud)", "AT SP 7"),
    ISO_14230_4_KWP_FAST("ISO 14230-4 KWP (Fast Init, 10.4 kbaud)", "AT SP 5"),
    ISO_14230_4_KWP_5BAUD("ISO 14230-4 KWP (5 baud init, 10.4 kbaud)", "AT SP 4"),
    ISO_9141_2("ISO 9141-2 (5 baud init, 10.4 kbaud)", "AT SP 3"),
    SAE_J1850_PWM("SAE J1850 PWM (41.6 kbaud)", "AT SP 1"),
    SAE_J1850_VPW("SAE J1850 VPW (10.4 kbaud)", "AT SP 2")
}

data class EcuProtocol(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val standard: ProtocolStandard,
    val targetAddressHex: String? = null, // e.g. "11" or "10" for K-Line ECUs
    val testerAddressHex: String? = "F1",
    val customHeaderCommand: String? = null, // e.g. "AT SH 81 11 F1"
    val wakeUpMessageCommand: String? = null, // e.g. "AT WM 81 11 F1 81"
    val initSequence: List<String> = emptyList(),
    val notesFa: String = ""
)
