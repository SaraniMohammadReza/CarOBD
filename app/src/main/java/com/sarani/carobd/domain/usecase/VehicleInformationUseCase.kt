package com.sarani.carobd.domain.usecase

import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.protocol.obd2.*
import com.sarani.carobd.domain.repository.ObdRepository

data class VehicleInfoReading(val pid: Int, val reading: DiagnosticReading<VehicleInfoField>)
class VehicleInformationUseCase(private val repository: ObdRepository) {
    val sourceLabel: String get() = (repository.connectionState.value as? ConnectionState.Connected)?.let { if (it.isSimulator) "SIMULATOR" else "PHYSICAL" } ?: "DISCONNECTED"
    suspend operator fun invoke(): List<VehicleInfoReading> {
        val token = repository.foregroundSessionId.value ?: return listOf(VehicleInfoReading(0, DiagnosticReading(ReadingState.Disconnected)))
        val output = mutableListOf<VehicleInfoReading>()
        suspend fun read(pid: Int) = repository.readEvidence("09%02X".format(pid), token).getOrElse {
            throw NotConnectedException(it.message ?: "Session unavailable")
        }
        try {
            val discovery = read(0)
            val support = discovery.associate { it.responderId to VehicleInfoDecoder.supported(it) }
            discovery.forEach { env ->
                val known = VehicleInfoDecoder.supported(env)
                output += VehicleInfoReading(0, if (known != null)
                    DiagnosticReading(ReadingState.Success, VehicleInfoField(0, known.map { "%02X".format(it) }), listOf(env))
                    else EvidenceDecoders.outcome<VehicleInfoField>(env) { throw IllegalArgumentException("Malformed information support bitmap") })
            }
            val counts = mutableMapOf<String?, Int>()
            for (pid in VehicleInfoDecoder.implementedPids) {
                if (support.values.none { it?.contains(pid) == true }) {
                    output += VehicleInfoReading(pid, DiagnosticReading(ReadingState.Unsupported, detail = "Not advertised by verified support bitmap"))
                    continue
                }
                val envelopes = read(pid)
                val readings = VehicleInfoDecoder.decode(pid, envelopes, if (pid == 2) counts else emptyMap())
                output += readings.map { VehicleInfoReading(pid, it) }
                if (pid == 1) readings.filter { it.state == ReadingState.Success }.forEach { r ->
                    r.value?.values?.singleOrNull()?.toIntOrNull()?.let { counts[r.evidence.firstOrNull()?.responderId] = it }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { output += VehicleInfoReading(0, DiagnosticReading(ReadingState.Disconnected, detail = e.message)) }
        return output
    }
}
