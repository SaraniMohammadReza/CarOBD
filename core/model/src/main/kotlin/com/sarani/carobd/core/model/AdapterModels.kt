package com.sarani.carobd.core.model

enum class AdapterType {
    GENUINE_ELM327,
    COMPATIBLE_CLONE_V15,
    COUNTERFEIT_CLONE_V21,
    STN11XX,
    STN21XX,
    OBDLINK_MX_PLUS,
    VIECAR_BLE4,
    VGATE_ICAR,
    UNKNOWN
}

enum class AdapterQuality(val titleFa: String, val descriptionFa: String) {
    GENUINE_OR_HIGH_QUALITY("بسیار باکیفیت / اورجینال", "چیپست واقعی PIC18F25K80 یا STN با پشتیبانی کامل از تمام دستورات و تایمینگ"),
    COMPATIBLE("سازگار و استاندارد", "پشتیبانی مناسب از پروتکل‌های استاندارد OBD-II و K-Line"),
    LIMITED("محدود", "پشتیبانی تنها از پروتکل‌های CAN بدون امکان تنظیم هدر اختصاصی K-Line"),
    SUSPICIOUS_CLONE("کلون مشکوک / فیک", "گزارش نسخه جعلی ۲.۱؛ فاقد بافر کافی و عدم پاسخ به دستورات AT تنظیم هدر"),
    BROKEN("معیوب / عدم پاسخگویی", "دانگل به دستورات پایه پاسخ نمی‌دهد یا ریست مکرر دارد")
}

data class AdapterCapabilities(
    val supportsVoltageReading: Boolean = true,
    val supportsCustomHeaders: Boolean = true,
    val supportsWakeupMessages: Boolean = true,
    val supportsFastBaudRates: Boolean = false,
    val supportsCanFiltering: Boolean = true,
    val supportsMsCan: Boolean = false,
    val supportsSwCan: Boolean = false,
    val supportsJ1939: Boolean = false,
    val maxBufferSize: Int = 256
)

data class AdapterQualityReport(
    val adapterType: AdapterType,
    val quality: AdapterQuality,
    val firmwareVersionString: String,
    val batteryVoltage: Float,
    val roundTripLatencyMs: Long,
    val detectedIssuesFa: List<String> = emptyList(),
    val isCloneDetected: Boolean = false,
    val heuristicRationale: String = ""
)
