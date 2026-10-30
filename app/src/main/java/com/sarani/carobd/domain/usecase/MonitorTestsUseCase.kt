package com.sarani.carobd.domain.usecase

import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.protocol.obd2.*
import com.sarani.carobd.domain.repository.ObdRepository

class MonitorTestsUseCase(private val repository: ObdRepository) {
    val sourceLabel get() = (repository.connectionState.value as? ConnectionState.Connected)?.let { if (it.isSimulator) "SIMULATOR" else "PHYSICAL" } ?: "DISCONNECTED"
    suspend operator fun invoke(mid: Int): List<DiagnosticReading<List<MonitorTestResult>>> {
        require(mid in 1..255 && mid % 32 != 0)
        val state = repository.connectionState.value as? ConnectionState.Connected ?: return listOf(DiagnosticReading(ReadingState.Disconnected))
        val supportedCan = listOf(ProtocolStandard.ISO_15765_4_CAN_11_500, ProtocolStandard.ISO_15765_4_CAN_29_500,
            ProtocolStandard.ISO_15765_4_CAN_11_250, ProtocolStandard.ISO_15765_4_CAN_29_250)
        if (supportedCan.none { it.displayName == state.protocolName }) return listOf(DiagnosticReading(ReadingState.Unsupported, detail = "CAN Mode06 layout only; non-CAN/O2 Mode05 is unavailable"))
        val token = repository.foregroundSessionId.value ?: return listOf(DiagnosticReading(ReadingState.Disconnected))
        suspend fun read(id: Int) = repository.readEvidence("06%02X".format(id), token).getOrElse { throw NotConnectedException(it.message ?: "Session changed") }
        try {
            var page = 0
            val discoveryFailures = mutableListOf<DiagnosticReading<List<MonitorTestResult>>>()
            while (page <= 0xE0) {
                val evidence = read(page)
                if (evidence.isEmpty()) return discoveryFailures + DiagnosticReading(ReadingState.Malformed, detail = "Empty Mode06 capability evidence")
                val support = evidence.mapNotNull { MonitorTestDecoder.supported(page, it) }.flatten().toSet()
                discoveryFailures += evidence.filter { MonitorTestDecoder.supported(page, it) == null }.map { e -> EvidenceDecoders.outcome<List<MonitorTestResult>>(e) { throw IllegalArgumentException("Malformed monitor support bitmap") } }
                if (mid in support) {
                    val results = read(mid)
                    return discoveryFailures + if (results.isEmpty()) listOf(DiagnosticReading(ReadingState.Malformed, detail = "Empty Mode06 response")) else results.map { MonitorTestDecoder.decode(mid, it) }
                }
                if (page + 32 !in support || mid <= page + 32) {
                    return discoveryFailures + DiagnosticReading(ReadingState.Unsupported, evidence = evidence, detail = "Monitor not advertised by verified bitmap")
                }
                page += 32
            }
            return listOf(DiagnosticReading(ReadingState.Unsupported))
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { return listOf(DiagnosticReading(ReadingState.Disconnected, detail = e.message)) }
    }
}
