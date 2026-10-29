package com.sarani.carobd.domain.usecase

import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.protocol.obd2.*
import com.sarani.carobd.domain.repository.ObdRepository

class FreezeFrameUseCase(private val repository: ObdRepository) {
    val sourceLabel: String get() = (repository.connectionState.value as? ConnectionState.Connected)?.let { if (it.isSimulator) "SIMULATOR" else "PHYSICAL" } ?: "DISCONNECTED"
    suspend operator fun invoke(frame: Int): List<DiagnosticReading<FreezeFrameValue>> {
        require(frame in 0..255)
        val token = repository.foregroundSessionId.value ?: return listOf(DiagnosticReading(ReadingState.Disconnected))
        val output = mutableListOf<DiagnosticReading<FreezeFrameValue>>()
        suspend fun read(pid: Int) = repository.readEvidence(FreezeFrameDecoder.command(pid, frame), token).getOrElse { throw NotConnectedException(it.message ?: "Session changed") }
        try {
            val discovery = read(0)
            val support = discovery.mapNotNull { FreezeFrameDecoder.supported(frame, it) }.flatten().toSet()
            discovery.forEach { e ->
                val supported = FreezeFrameDecoder.supported(frame, e)
                output += if (supported != null) DiagnosticReading(ReadingState.Success,
                    FreezeFrameValue(0, frame, null, null, supported.joinToString(" ") { "%02X".format(it) }), listOf(e), "Supported PIDs 01–20 for selected frame")
                    else EvidenceDecoders.outcome<FreezeFrameValue>(e) { throw IllegalArgumentException("Invalid freeze-frame support bitmap") }
            }
            for (pid in support.filter { it != 0x20 }.sorted()) {
                val envelopes = read(pid)
                if (envelopes.isEmpty()) output += DiagnosticReading(ReadingState.Malformed, detail = "Empty freeze-frame evidence")
                output += envelopes.map { FreezeFrameDecoder.decode(pid, frame, it) }
            }
            if (0x20 in support) output += DiagnosticReading(ReadingState.Unsupported, detail = "Extended freeze-frame PID pages are not decoded; base-page results only")
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { output += DiagnosticReading(ReadingState.Disconnected, detail = e.message) }
        return output
    }
}
