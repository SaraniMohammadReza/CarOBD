package com.sarani.carobd.domain.usecase

import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.protocol.obd2.EvidenceDecoders
import com.sarani.carobd.domain.repository.ObdRepository

class ReadinessUseCase(private val repository: ObdRepository) {
    val sourceLabel: String get() = (repository.connectionState.value as? ConnectionState.Connected)?.let { if (it.isSimulator) "SIMULATOR" else "PHYSICAL" } ?: "DISCONNECTED"
    suspend operator fun invoke(): List<DiagnosticReading<ReadinessStatus>> = repository.readEvidence("0101").fold(
        onSuccess = { EvidenceDecoders.readiness(it) },
        onFailure = { listOf(DiagnosticReading(if (it is NotConnectedException) ReadingState.Disconnected else ReadingState.AdapterFailure, detail = it.message)) })
}
