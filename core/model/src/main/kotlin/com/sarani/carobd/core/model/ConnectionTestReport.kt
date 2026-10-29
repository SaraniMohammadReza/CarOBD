package com.sarani.carobd.core.model

enum class ConnectionTestStage { TRANSPORT, ADAPTER, IDENTITY, PROTOCOL, ECU, CAPABILITIES }
enum class ConnectionTestStatus { PENDING, RUNNING, PASS, FAIL, SKIPPED }
data class ConnectionTestEvidence(val stage: ConnectionTestStage, val status: ConnectionTestStatus, val evidence: String)
data class ConnectionTestReport(
    val isSimulator: Boolean,
    val stages: List<ConnectionTestEvidence> = ConnectionTestStage.entries.map { ConnectionTestEvidence(it, ConnectionTestStatus.PENDING, "") },
) {
    val passed: Boolean get() = stages.all { it.status == ConnectionTestStatus.PASS }
    fun record(event: ConnectionTestEvidence): ConnectionTestReport = copy(stages = stages.map { if (it.stage == event.stage) event else it })
    fun finishFailure(reason: String): ConnectionTestReport {
        val failed = stages.firstOrNull { it.status == ConnectionTestStatus.RUNNING }?.stage
            ?: stages.firstOrNull { it.status == ConnectionTestStatus.PENDING }?.stage
        return copy(stages = stages.map {
            when {
                it.status == ConnectionTestStatus.PASS || it.status == ConnectionTestStatus.FAIL -> it
                it.stage == failed -> it.copy(status = ConnectionTestStatus.FAIL, evidence = reason)
                else -> it.copy(status = ConnectionTestStatus.SKIPPED, evidence = "Not completed: $reason")
            }
        })
    }
}
