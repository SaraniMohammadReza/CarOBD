package com.sarani.carobd.core.telemetry.scheduler

import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.PidCapabilityMap
import com.sarani.carobd.core.model.TelemetryPollMode
import com.sarani.carobd.core.protocol.obd2.PidCodec
import com.sarani.carobd.core.protocol.obd2.PidPollingPlan
import com.sarani.carobd.core.telemetry.fuel.FuelEconomyCalculator
import com.sarani.carobd.core.telemetry.trip.TripComputer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * §5 single-producer telemetry engine.
 *
 * Exactly ONE coroutine drives ALL polling for a connection session.
 * Consumers observe [liveTelemetry] (a hot StateFlow); they never start their
 * own polling loop. The legacy repo implementation returned a cold `flow {}`
 * AND launched an internal collector of that same cold flow — collecting it
 * twice produced duplicate polling loops. This engine removes that hazard:
 * there is one [startIn] job and one hot state stream.
 *
 * §7: pacing is driven by the persisted [TelemetryPollMode] (ECO/NORMAL/FAST).
 * §9/§22: capability discovery is lazy (extension-bit) and the poll plan is
 * capability-gated, so unsupported PIDs are never repeatedly requested.
 * §8: fuel rate is computed only from measured MAF (displacement/VE are never
 * assumed), so telemetry is never fabricated as zero.
 */
class TelemetryProducer(
    private val pollMode: StateFlow<TelemetryPollMode>,
    private val executeCommand: suspend (command: String, timeoutMs: Long) -> String?,
    private val isConnected: () -> Boolean,
    private val tripComputer: TripComputer? = null,
    private val onTelemetry: (LiveTelemetry) -> Unit = {},
) {

    private val _liveTelemetry = MutableStateFlow(LiveTelemetry())
    val liveTelemetry: StateFlow<LiveTelemetry> = _liveTelemetry.asStateFlow()

    private var job: Job? = null

    /** True while a producer is running. Read-only view for tests/UI observers. */
    val isRunning: Boolean get() = job?.isActive == true

    /** The active producer job, or null. Exposed for reconnect/cancellation tests. */
    internal val activeJob: Job? get() = job

    /**
     * Launches the single producer on [scope]. Calling this again (e.g. on
     * reconnect) cancels the previous producer first, so at most ONE producer is
     * ever running per connection session. Returns the new [Job].
     */
    fun startIn(scope: CoroutineScope): Job {
        job?.cancel()
        job = scope.launch { produceLoop() }
        return job!!
    }

    /** Cancels the active producer job. Consumers' StateFlow observation is unaffected. */
    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun produceLoop() {
        // §8: vehicle metadata (engine displacement / VE) is NOT carried on
        // LiveTelemetry, so fuel rate is MAF-only; never assume TU5/VE=0.80.
        var capabilityMap: PidCapabilityMap? = null
        var atsrvCycle = 0L
        while (coroutineContext.isActive && isConnected()) {
            val delayMs = pollMode.value.delayMs
            try {
                var current = _liveTelemetry.value
                val measured = current.measuredPids.toMutableSet()

                // §9/§22: discover capability once per session (lazy extension bit).
                if (capabilityMap == null) {
                    capabilityMap = runCatching {
                        PidPollingPlan.discover { cmd ->
                            executeCommand(cmd, 2000L)?.takeIf {
                                // treat 7F/NO DATA as absent (null)
                                val b = com.sarani.carobd.core.protocol.obd2.PidCodec.cleanResponseHex(it)
                                b.size >= 6 && (b[0].toInt() and 0xFF) == 0x41
                            }
                        }
                    }.getOrDefault(PidCapabilityMap())
                }

                val pollPlan = PidPollingPlan.supported(capabilityMap)

                // Periodic adapter battery voltage (ATRV), decoupled from vehicle PID
                // pacing — §20: UI redraw may differ from vehicle polling rate.
                atsrvCycle++
                if (atsrvCycle % 15 == 0L) {
                    val v = executeCommand("ATRV", 500L)?.let { raw ->
                        val num = raw.replace("V", "", ignoreCase = true).trim()
                            .toFloatOrNull()
                        num?.takeIf { it > 0f }
                    }
                    if (v != null) {
                        measured.add("ATRV")
                        current = current.copy(batteryVoltage = v)
                    }
                }

                // Capability-gated live PID polling.
                for (spec in pollPlan) {
                    val raw = executeCommand(spec.command, 600L) ?: continue
                    val bytes = com.sarani.carobd.core.protocol.obd2.PidCodec.cleanResponseHex(raw)
                    PidPollingPlan.decodePoll(spec, bytes, current)?.let { next ->
                        measured.add(spec.pidHex)
                        current = next
                    }
                }

                // §8 truthfulness: fuel rate ONLY from measured MAF.
                val fuelRateLph = FuelEconomyCalculator.fuelRateIfMeasurable(
                    mafGramsPerSec = if ("10" in measured) current.mafGramsPerSec.toDouble() else null,
                    rpm = if ("0C" in measured && current.rpm > 0) current.rpm.toDouble() else null,
                    mapKpa = if ("0B" in measured && current.intakeManifoldPressureKpa > 0) current.intakeManifoldPressureKpa.toDouble() else null,
                    iatCelsius = if ("0F" in measured) current.intakeAirTempCelsius.toDouble() else null,
                    displacementLiters = null,   // §8: not available -> never assume TU5/1.6
                    volumetricEfficiency = null // §8: unknown -> never assume 0.80
                )

                tripComputer?.processSample(
                    speedKmh = current.speedKmh,
                    rpm = current.rpm,
                    fuelRateLitersPerHour = fuelRateLph ?: 0.0,
                    timestampMs = System.currentTimeMillis()
                )

                current = current.copy(measuredPids = measured, timestamp = System.currentTimeMillis())
                _liveTelemetry.value = current
                onTelemetry(current)

                kotlinx.coroutines.delay(delayMs)
            } catch (_: Exception) {
                // Transient adapter error: brief back-off, do not kill the producer
                // (the connection predicate / stop handles termination).
                kotlinx.coroutines.delay(500L)
            }
        }
    }
}
