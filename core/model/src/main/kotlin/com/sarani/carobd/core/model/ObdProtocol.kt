package com.sarani.carobd.core.model

enum class ProtocolStandard(
    val id: String,
    val displayName: String,
    val elmCommand: String,
    val baudRate: Int,
    val isCanBus: Boolean,
    val maturity: FeatureMaturity = FeatureMaturity.PRODUCTION_IMPLEMENTED
) {
    AUTO("auto", "Automatic Search (ELM327)", "AT SP 0", 0, false),
    SAE_J1850_PWM("j1850_pwm", "SAE J1850 PWM (41.6 kbaud)", "AT SP 1", 41600, false),
    SAE_J1850_VPW("j1850_vpw", "SAE J1850 VPW (10.4 kbaud)", "AT SP 2", 10400, false),
    ISO_9141_2("iso9141_2", "ISO 9141-2 (5 baud init, 10.4 kbaud)", "AT SP 3", 10400, false),
    ISO_14230_4_KWP_5BAUD("kwp_5baud", "ISO 14230-4 KWP (5 baud init, 10.4 kbaud)", "AT SP 4", 10400, false),
    ISO_14230_4_KWP_FAST("kwp_fast", "ISO 14230-4 KWP (Fast Init, 10.4 kbaud)", "AT SP 5", 10400, false),
    ISO_15765_4_CAN_11_500("can_11_500", "ISO 15765-4 CAN (11 bit ID, 500 kbaud)", "AT SP 6", 500000, true),
    ISO_15765_4_CAN_29_500("can_29_500", "ISO 15765-4 CAN (29 bit ID, 500 kbaud)", "AT SP 7", 500000, true),
    ISO_15765_4_CAN_11_250("can_11_250", "ISO 15765-4 CAN (11 bit ID, 250 kbaud)", "AT SP 8", 250000, true),
    ISO_15765_4_CAN_29_250("can_29_250", "ISO 15765-4 CAN (29 bit ID, 250 kbaud)", "AT SP 9", 250000, true),

    // Advanced & Second-Generation Protocols
    ISO_14229_UDS_CAN("uds_can", "ISO 14229 UDS on CAN (ISO-TP)", "AT SP 6", 500000, true, FeatureMaturity.IMPLEMENTED_NEEDS_HARDWARE_VALIDATION),
    SAE_J1939_CAN("j1939_can", "SAE J1939 Heavy-Duty (29 bit, 250 kbaud)", "AT SP A", 250000, true, FeatureMaturity.IMPLEMENTED_NEEDS_HARDWARE_VALIDATION),
    ISO_27145_WWH_OBD("wwh_obd", "ISO 27145 WWH-OBD (World-Wide Harmonized)", "AT SP 6", 500000, true, FeatureMaturity.EXPERIMENTAL),
    SAE_J1979_2_OBD_ON_UDS("obd_on_uds", "SAE J1979-2 OBDonUDS (2nd Gen OBD)", "AT SP 6", 500000, true, FeatureMaturity.EXPERIMENTAL),
    SAE_J1979_3_ZEV_ON_UDS("zev_on_uds", "SAE J1979-3 ZEVonUDS (EV/Hybrid Diagnostics)", "AT SP 6", 500000, true, FeatureMaturity.EXPERIMENTAL)
}

data class EcuHeader(
    val priorityHex: String = "81",
    val targetAddressHex: String = "11",
    val testerAddressHex: String = "F1"
) {
    fun toElmHeaderCommand(): String = "AT SH $priorityHex $targetAddressHex $testerAddressHex"
    fun toElmWakeupCommand(): String = "AT WM $priorityHex $targetAddressHex $testerAddressHex $priorityHex"
}

data class EcuProtocolConfig(
    val standard: ProtocolStandard,
    val header: EcuHeader? = null,
    val customCanFilterHex: String? = null,
    val customCanMaskHex: String? = null,
    val customInitCommands: List<String> = emptyList(),
    val responseTimeoutMs: Long = 300L,
    val confidence: ConfidenceLevel = ConfidenceLevel.VERIFIED_STANDARD
)
