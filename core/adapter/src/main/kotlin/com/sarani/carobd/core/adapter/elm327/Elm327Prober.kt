package com.sarani.carobd.core.adapter.elm327

import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.model.AdapterCapabilities
import com.sarani.carobd.core.model.AdapterQuality

/**
 * Diagnostic prober to determine ELM327 / STN hardware capabilities,
 * detect fake/clone microcontrollers, and measure battery voltage.
 */
class Elm327Prober(
    private val commandExecutor: suspend (command: String, timeoutMs: Long) -> AdapterResponse
) {

    suspend fun probeHardware(): HardwareProbeResult {
        var firmwareVersion = "Unknown"
        var deviceDescription = ""
        var voltage = 0.0f
        var supportsAtat = false
        var supportsCsm = false
        var supportsCaf = false
        var supportsStnCommands = false
        var isCloneLikely = false

        // 1. Probe ATI
        val atiResp = commandExecutor("ATI", 1500L)
        if (atiResp is AdapterResponse.Success && atiResp.cleanedLines.isNotEmpty()) {
            firmwareVersion = atiResp.cleanedLines.first()
        }

        // 2. Probe AT@1
        val at1Resp = commandExecutor("AT@1", 1000L)
        if (at1Resp is AdapterResponse.Success && at1Resp.cleanedLines.isNotEmpty()) {
            deviceDescription = at1Resp.cleanedLines.first()
        }

        // 3. Probe ATRV (Battery Voltage)
        val atrvResp = commandExecutor("ATRV", 1000L)
        if (atrvResp is AdapterResponse.Success && atrvResp.cleanedLines.isNotEmpty()) {
            val vStr = atrvResp.cleanedLines.first().replace("V", "").trim()
            voltage = vStr.toFloatOrNull() ?: 0.0f
        }

        // 4. Test Adaptive Timing (ATAT1)
        val atatResp = commandExecutor("ATAT1", 1000L)
        supportsAtat = atatResp is AdapterResponse.Success

        // 5. Test CAN Auto Formatting (ATCAF1)
        val cafResp = commandExecutor("ATCAF1", 1000L)
        supportsCaf = cafResp is AdapterResponse.Success

        // 6. Test CAN Silent Monitoring (ATCSM0)
        val csmResp = commandExecutor("ATCSM0", 1000L)
        supportsCsm = csmResp is AdapterResponse.Success

        // 7. Test STN command set (STPBR or STI)
        val stiResp = commandExecutor("STI", 1000L)
        if (stiResp is AdapterResponse.Success && !stiResp.rawResponse.contains("?")) {
            supportsStnCommands = true
        }

        // Evaluate clone status:
        if (firmwareVersion.contains("v2.1", ignoreCase = true) && (!supportsCaf || !supportsAtat)) {
            isCloneLikely = true
        }

        val quality = when {
            supportsStnCommands -> AdapterQuality.GENUINE_OR_HIGH_QUALITY
            isCloneLikely -> AdapterQuality.SUSPICIOUS_CLONE
            supportsCaf && supportsAtat -> AdapterQuality.GENUINE_OR_HIGH_QUALITY
            else -> AdapterQuality.COMPATIBLE
        }

        val capabilities = AdapterCapabilities(
            supportsVoltageReading = true,
            supportsCustomHeaders = supportsCaf,
            supportsWakeupMessages = true,
            supportsFastBaudRates = supportsStnCommands,
            supportsCanFiltering = supportsCsm,
            supportsMsCan = supportsStnCommands,
            supportsSwCan = supportsStnCommands,
            supportsJ1939 = supportsStnCommands || supportsCaf,
            maxBufferSize = if (supportsStnCommands) 2048 else 256
        )

        return HardwareProbeResult(
            firmwareVersion = firmwareVersion,
            deviceDescription = deviceDescription,
            voltage = voltage,
            capabilities = capabilities,
            quality = quality,
            isClone = isCloneLikely
        )
    }
}

data class HardwareProbeResult(
    val firmwareVersion: String,
    val deviceDescription: String,
    val voltage: Float,
    val capabilities: AdapterCapabilities,
    val quality: AdapterQuality,
    val isClone: Boolean
)
