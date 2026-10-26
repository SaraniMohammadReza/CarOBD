package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.model.AdapterCapabilities
import com.sarani.carobd.core.model.AdapterQuality
import com.sarani.carobd.core.model.ProtocolStandard

/**
 * Result of executing an AT or OBD command through the adapter.
 */
sealed class AdapterResponse {
    data class Success(
        val rawResponse: String,
        val cleanedLines: List<String>,
        val executionTimeMs: Long
    ) : AdapterResponse()

    data class Error(
        val error: AdapterError,
        val rawResponse: String,
        val executionTimeMs: Long
    ) : AdapterResponse()
}

/**
 * ELM327 / STN hardware error classification.
 */
sealed class AdapterError(val messageEn: String, val messageFa: String) {
    object NoData : AdapterError("No data received from vehicle ECU (PID not supported or engine off)", "پاسخی از کامپیوتر خودرو دریافت نشد (سنسور پشتیبانی نمی‌شود یا سوئیچ بسته است)")
    object UnableToConnect : AdapterError("Unable to connect to vehicle OBD bus", "اتصال به شبکه ارتباطی خودرو ناموفق بود")
    object BusInitFailed : AdapterError("Bus initialization failed", "مقداردهی اولیه خط ارتباطی با خطا مواجه شد")
    object BusBusy : AdapterError("OBD communication bus is busy", "خط ارتباطی خودرو مشغول است")
    object CanError : AdapterError("CAN bus hardware error detected", "خطای سخت‌افزاری در گذرگاه CAN رخ داد")
    object Stopped : AdapterError("Adapter execution stopped by user or interrupt", "اجرای دستور توسط وقفه متوقف شد")
    object BufferOverflow : AdapterError("Adapter RX/TX buffer overflow", "سرریز بافر دریافت/ارسال آداپتور")
    object LowVoltageReset : AdapterError("Adapter suffered low voltage reset", "افت شدید ولتاژ و ریست شدن آداپتور")
    object UnknownCommand : AdapterError("Command not recognized by adapter (?)", "دستور برای آداپتور ناشناخته است (?)")
    data class Timeout(val timeoutMs: Long) : AdapterError("Command execution timed out after ${timeoutMs}ms", "پاسخ در زمان مجاز دریافت نشد (${timeoutMs} میلی‌ثانیه)")
    data class TransportFault(val detail: String) : AdapterError("Transport layer fault: $detail", "خطای بستر ارتباطی: $detail")
    data class Other(val raw: String) : AdapterError("Unrecognized adapter response: $raw", "پاسخ ناشناخته از آداپتور: $raw")
}

/**
 * Result of initial adapter handshake and probe.
 */
data class AdapterInitializationResult(
    val isSuccess: Boolean,
    val firmwareVersion: String,
    val detectedProtocol: ProtocolStandard?,
    val batteryVoltage: Float,
    val capabilities: AdapterCapabilities,
    val quality: AdapterQuality,
    val errorMessage: String? = null
)
