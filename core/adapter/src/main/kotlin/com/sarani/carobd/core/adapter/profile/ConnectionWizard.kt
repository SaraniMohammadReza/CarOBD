package com.sarani.carobd.core.adapter.profile

import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.TransportType

/**
 * §20 STAGED TEST-CONNECTION STATE MACHINE (pure, transport-agnostic, testable).
 *
 * Maps the adapter's truthful [ConnectionState] trace into the six §20 stages,
 * each with an explicit status and the evidence observed along the trace.
 * Evidence from earlier stages (e.g. adapter identity seen during
 * AdapterIdentifying) is accumulated so it survives later state transitions.
 *
 * Stage ordering (§20):
 *   1 TRANSPORT  — physical channel open
 *   2 ADAPTER    — adapter responded (AT-echo / reset)
 *   3 IDENTITY   — ATI/ATRV identity
 *   4 PROTOCOL   — ATDP/ATDPN negotiated protocol
 *   5 ECU        — Mode 01 PID 00 positive (41 00)
 *   6 CAPABILITY — supported-PID discovery
 */
@Deprecated("Legacy state projection is not verification. Use explicit ConnectionTestReport evidence from the executor.")
object ConnectionWizard {

    enum class Stage(val index: Int) {
        TRANSPORT(0), ADAPTER(1), IDENTITY(2), PROTOCOL(3), ECU(4), CAPABILITY(5);
        val title: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
    }

    data class StageStatus(
        val stage: Stage,
        val state: StageState,
        val evidence: String? = null,
    )

    enum class StageState { PENDING, IN_PROGRESS, PASS, FAIL, SKIPPED }

    /**
     * @param trace ordered [ConnectionState] values emitted during connect (the
     *   first element is the earliest). Evidence is accumulated across the trace.
     * @return the six stages in order with status + evidence.
     */
    fun evaluate(
        transportType: TransportType,
        address: String?,
        trace: List<ConnectionState>,
    ): List<StageStatus> {
        val failure = trace.filterIsInstance<ConnectionState.Failure>().lastOrNull()
        val failStage = failure?.let { failureStage(it.stage) }
        // When a stage has failed, the Failure.stage tells us how far we got:
        // stages before it are PASS, the failing stage is FAIL, the rest PENDING.
        val (completedUpTo, inProgress) = if (failure != null) {
            Pair((failStage?.index ?: 0) - 1, null as Stage?)
        } else {
            val current = trace.lastOrNull() ?: ConnectionState.Disconnected
            progressOf(current)
        }
        return Stage.entries.map { stage ->
            val st = when {
                failStage != null && stage.index == failStage.index -> StageState.FAIL
                failStage != null && stage.index < failStage.index -> StageState.PASS
                failStage != null -> StageState.PENDING
                stage.index <= completedUpTo -> StageState.PASS
                stage == inProgress -> StageState.IN_PROGRESS
                else -> StageState.PENDING
            }
            StageStatus(stage, st, evidenceFor(stage, transportType, address, trace))
        }
    }

    /** Leading in-progress stage, or the last stage when everything has passed. */
    fun currentStage(statuses: List<StageStatus>): Stage =
        statuses.firstOrNull { it.state == StageState.IN_PROGRESS }?.stage
            ?: statuses.last().stage

    private fun progressOf(state: ConnectionState): Pair<Int, Stage?> = when (state) {
        is ConnectionState.TransportConnected -> Pair(0, Stage.ADAPTER)
        is ConnectionState.TransportConnecting -> Pair(-1, null)
        is ConnectionState.AdapterResetting -> Pair(0, Stage.ADAPTER)
        is ConnectionState.AdapterIdentifying,
        is ConnectionState.AdapterConfiguring -> Pair(1, Stage.IDENTITY)
        is ConnectionState.ProtocolDetecting -> Pair(2, Stage.PROTOCOL)
        is ConnectionState.EcuDiscovering -> Pair(3, Stage.ECU)
        is ConnectionState.CapabilityDiscovering -> Pair(4, Stage.CAPABILITY)
        is ConnectionState.Connected -> Pair(5, null)
        is ConnectionState.Idle,
        is ConnectionState.Disconnected,
        is ConnectionState.Disconnecting,
        is ConnectionState.PermissionRequired,
        is ConnectionState.TransportScanning,
        is ConnectionState.DeviceSelected,
        is ConnectionState.Degraded,
        is ConnectionState.Retrying,
        is ConnectionState.Failure -> Pair(-1, null)
    }

    private fun failureStage(stage: String): Stage = when (stage.uppercase()) {
        "BLUETOOTH" -> Stage.TRANSPORT
        "ADAPTER" -> Stage.ADAPTER
        "PROTOCOL" -> Stage.PROTOCOL
        "ECU" -> Stage.ECU
        "CAPABILITY" -> Stage.CAPABILITY
        else -> Stage.TRANSPORT
    }

    @Suppress("unused")
    private fun evidenceFor(stage: Stage, transportType: TransportType, address: String?, trace: List<ConnectionState>): String? =
        when (stage) {
            Stage.TRANSPORT -> transportLabel(transportType, address)
            Stage.ADAPTER -> trace.filterIsInstance<ConnectionState.AdapterConfiguring>().lastOrNull()?.setting
                ?: trace.filterIsInstance<ConnectionState.AdapterResetting>().lastOrNull()?.let { "reset" }
            Stage.IDENTITY -> trace.filterIsInstance<ConnectionState.Connected>().lastOrNull()?.adapterType
                ?: trace.filterIsInstance<ConnectionState.AdapterIdentifying>().lastOrNull()?.probeStage
            Stage.PROTOCOL -> trace.filterIsInstance<ConnectionState.Connected>().lastOrNull()?.protocolName
                ?: trace.filterIsInstance<ConnectionState.ProtocolDetecting>().lastOrNull()?.candidateName
                ?: trace.filterIsInstance<ConnectionState.EcuDiscovering>().lastOrNull()?.protocolName
            Stage.ECU -> if (trace.any { it is ConnectionState.Connected }) "41 00"
            else trace.filterIsInstance<ConnectionState.CapabilityDiscovering>().lastOrNull()?.let { "41 00" }
            Stage.CAPABILITY -> trace.filterIsInstance<ConnectionState.Connected>().lastOrNull()
                ?.let { "${it.responderCount} responder(s)" }
                ?: trace.filterIsInstance<ConnectionState.CapabilityDiscovering>().lastOrNull()
                ?.let { "${it.supportedPidCount} PIDs supported" }
        }

    private fun transportLabel(transportType: TransportType, address: String?): String? = when (transportType) {
        TransportType.BLUETOOTH_CLASSIC -> "Bluetooth Classic $address"
        TransportType.BLUETOOTH_LE -> "Bluetooth LE $address"
        TransportType.WIFI_TCP -> "Wi-Fi $address"
        TransportType.USB_SERIAL -> "USB $address"
        TransportType.SIMULATOR -> "Simulator"
    }
}
