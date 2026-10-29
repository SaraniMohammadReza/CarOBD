package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*

object EvidenceDecoders {
    fun readiness(envelopes: List<DiagnosticWireResponse>): List<DiagnosticReading<ReadinessStatus>> =
        if (envelopes.isEmpty()) listOf(DiagnosticReading(ReadingState.Malformed, detail = "Empty readiness evidence")) else envelopes.map { readiness(it) }

    fun <T> outcome(envelope: DiagnosticWireResponse, decode: (ByteArray) -> T): DiagnosticReading<T> {
        val evidence = listOf(envelope)
        return when (val result = envelope.result) {
            is DiagnosticResult.Positive -> try { DiagnosticReading(ReadingState.Success, decode(result.payload), evidence) }
                catch (e: IllegalArgumentException) { DiagnosticReading(ReadingState.Malformed, evidence = evidence, detail = e.message) }
            is DiagnosticResult.NoData -> DiagnosticReading(ReadingState.NoData, evidence = evidence)
            is DiagnosticResult.NegativeResponse -> DiagnosticReading(ReadingState.NegativeResponse, evidence = evidence, detail = result.nrc?.let { "%02X".format(it) })
            DiagnosticResult.Timeout -> DiagnosticReading(ReadingState.Timeout, evidence = evidence)
            is DiagnosticResult.Partial -> DiagnosticReading(ReadingState.Partial, evidence = evidence)
            is DiagnosticResult.Unsupported -> DiagnosticReading(ReadingState.Unsupported, evidence = evidence)
            is DiagnosticResult.AdapterFailure -> DiagnosticReading(ReadingState.AdapterFailure, evidence = evidence, detail = result.detail)
            is DiagnosticResult.Malformed -> DiagnosticReading(ReadingState.Malformed, evidence = evidence, detail = result.reason)
        }
    }
    fun readiness(envelope: DiagnosticWireResponse): DiagnosticReading<ReadinessStatus> = outcome(envelope) { payload ->
        require(envelope.service == DiagnosticService.MODE_01 && payload.size == 5 && payload[0] == 1.toByte()) { "Readiness SID/PID/length mismatch" }
        PidCodec.decodeReadinessStatus(payload.copyOfRange(1, 5))
    }
}
