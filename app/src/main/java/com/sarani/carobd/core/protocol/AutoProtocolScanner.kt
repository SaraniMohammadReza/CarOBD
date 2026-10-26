package com.sarani.carobd.core.protocol

import com.sarani.carobd.core.connectivity.ObdConnection
import com.sarani.carobd.core.model.EcuProtocol
import com.sarani.carobd.core.protocol.iranian.IranianEcuRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface ProtocolScanStep {
    data class Testing(val protocol: EcuProtocol, val index: Int, val total: Int) : ProtocolScanStep
    data class Success(val detectedProtocol: EcuProtocol) : ProtocolScanStep
    data class Failed(val error: String) : ProtocolScanStep
}

class AutoProtocolScanner(
    private val connection: ObdConnection
) {

    private val scanCandidates = listOf(
        // 1. High Speed CAN 500kbps (Tara, Dena+, EcoMux, Shahin, Modern Global)
        IranianEcuRegistry.PROTOCOL_EASYU_CAN,
        IranianEcuRegistry.PROTOCOL_BOSCH_ME749,

        // 2. K-Line KWP2000 Fast Init with Siemens / Crouse headers (Pride, Tiba, 405, Pars)
        IranianEcuRegistry.PROTOCOL_SIEMENS,

        // 3. K-Line KWP2000 Fast Init with Bosch ME7.4.4 headers (206 TU5, 207, Pars TU5)
        IranianEcuRegistry.PROTOCOL_BOSCH_ME744,

        // 4. K-Line SSAT KWP2000
        IranianEcuRegistry.PROTOCOL_SSAT,

        // 5. K-Line ISO 9141-2 5-Baud (Valeo S2000 / Sagem)
        IranianEcuRegistry.PROTOCOL_VALEO_S2000,

        // 6. Generic Auto Detect
        IranianEcuRegistry.PROTOCOL_GENERIC_AUTO
    )

    fun scanProtocols(): Flow<ProtocolScanStep> = flow {
        if (!connection.isConnected()) {
            emit(ProtocolScanStep.Failed("ارتباط با سخت‌افزار دیاگ برقرار نیست"))
            return@flow
        }

        // Reset adapter
        connection.sendCommand("ATZ")
        delay(200)
        connection.sendCommand("ATE0")
        connection.sendCommand("ATL0")

        val total = scanCandidates.size
        for ((index, candidate) in scanCandidates.withIndex()) {
            emit(ProtocolScanStep.Testing(candidate, index + 1, total))

            // Apply candidate's init sequence
            for (cmd in candidate.initSequence) {
                connection.sendCommand(cmd)
                delay(35)
            }

            // Probe with standard Mode 01 PID 00 (Supported PIDs)
            val probeResult = connection.sendCommand("0100")
            if (probeResult.isSuccess) {
                val raw = probeResult.getOrNull() ?: ""
                val cleaned = FrameParser.cleanResponse(raw)

                if (!FrameParser.isErrorOrNoData(cleaned) && cleaned.contains("41 00")) {
                    emit(ProtocolScanStep.Success(candidate))
                    return@flow
                }
            }

            // Also test with 010C (RPM) as secondary verification
            val rpmProbe = connection.sendCommand("010C")
            if (rpmProbe.isSuccess) {
                val raw = rpmProbe.getOrNull() ?: ""
                val cleaned = FrameParser.cleanResponse(raw)
                if (!FrameParser.isErrorOrNoData(cleaned) && cleaned.contains("41 0C")) {
                    emit(ProtocolScanStep.Success(candidate))
                    return@flow
                }
            }

            delay(60)
        }

        // Fallback to generic auto protocol if nothing matched strictly
        emit(ProtocolScanStep.Success(IranianEcuRegistry.PROTOCOL_GENERIC_AUTO))
    }
}
