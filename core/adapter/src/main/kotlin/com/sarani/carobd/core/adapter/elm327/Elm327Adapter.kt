package com.sarani.carobd.core.adapter.elm327

import com.sarani.carobd.core.adapter.ObdAdapter
import com.sarani.carobd.core.adapter.command.AdapterError
import com.sarani.carobd.core.adapter.command.AdapterInitializationResult
import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ProtocolStandard
import com.sarani.carobd.core.protocol.obd2.PidCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Robust ELM327 / STN communication adapter.
 * Handles state machine synchronization, buffer parsing, timeout policies,
 * protocol negotiation, and hardware health monitoring.
 */
open class Elm327Adapter(
    val transport: ObdTransport,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : ObdAdapter {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var _currentProtocol: ProtocolStandard? = null
    override val currentProtocol: ProtocolStandard?
        get() = _currentProtocol

    private var _batteryVoltage: Float = 0.0f
    override val batteryVoltage: Float
        get() = _batteryVoltage

    private val commandMutex = Mutex()
    private val incomingCharsChannel = Channel<Char>(Channel.UNLIMITED)
    private var readerJob: Job? = null

    init {
        startIncomingReader()
    }

    private fun startIncomingReader() {
        readerJob?.cancel()
        readerJob = scope.launch {
            try {
                transport.incomingBytes.collect { chunk ->
                    for (b in chunk) {
                        incomingCharsChannel.send(b.toInt().toChar())
                    }
                }
            } catch (_: CancellationException) {
                // Normal job shutdown
            } catch (e: Exception) {
                _state.value = ConnectionState.Failure(
                    stage = "STREAM_READ",
                    humanMessageFa = "خطا در دریافت جریان داده از آداپتور",
                    technicalMessage = e.message ?: "Unknown stream fault",
                    isRecoverable = true
                )
            }
        }
    }

    override suspend fun initialize(forcedProtocol: ProtocolStandard?): AdapterInitializationResult =
        commandMutex.withLock {
            try {
                _state.value = ConnectionState.TransportConnecting(transport.transportType.name)
                if (!transport.isConnected) {
                    transport.connect()
                }

                _state.value = ConnectionState.AdapterResetting
                drainPendingBuffer()

                // 1. Reset chip: ATZ
                executeInternal("ATZ", 2500L)
                kotlinx.coroutines.delay(200)
                drainPendingBuffer()

                // 2. Disable echo: ATE0
                executeInternal("ATE0", 1000L)

                // 3. Disable linefeeds: ATL0
                executeInternal("ATL0", 1000L)

                // 4. Disable spaces for faster transmission: ATS0
                executeInternal("ATS0", 1000L)

                // 5. Adaptive timing auto 1: ATAT1
                executeInternal("ATAT1", 1000L)

                // 6. Run hardware probe (ATI, ATRV, Clone check)
                val prober = Elm327Prober { cmd, timeout -> executeInternal(cmd, timeout) }
                val probeResult = prober.probeHardware()
                _batteryVoltage = probeResult.voltage

                // 7. Negotiate or force protocol
                _state.value = ConnectionState.ProtocolDetecting(
                    candidateName = forcedProtocol?.displayName ?: "Auto Search",
                    attemptIndex = 1,
                    totalCandidates = 1
                )
                if (forcedProtocol != null) {
                    val atspCmd = getElmProtocolCommand(forcedProtocol)
                    executeInternal(atspCmd, 1500L)
                    _currentProtocol = forcedProtocol
                } else {
                    executeInternal("ATSP0", 1500L)
                }

                // 8. Issue first query (0100) to trigger vehicle handshake.
                // Require real, service-correlated ECU evidence: only a valid Mode 01 PID 00
                // response (0x41 0x00 followed by >=4 supported-PID data bytes) may advance
                // to Connected. Any adapter/ECU error, timeout, or malformed frame must fail
                // initialization; receiving the prompt alone is not proof of communication.
                val init0100 = executeInternal("0100", 4000L)
                when {
                    init0100 is AdapterResponse.Error -> {
                        val err = init0100.error
                        _state.value = ConnectionState.Failure(
                            stage = "ECU_HANDSHAKE",
                            humanMessageFa = "ارتباط با کامپیوتر خودرو برقرار نشد",
                            technicalMessage = "0100 failed: ${err.messageEn}",
                            isRecoverable = err !is AdapterError.NoData && err !is AdapterError.UnknownCommand,
                            rawResponse = init0100.rawResponse
                        )
                        return AdapterInitializationResult(
                            isSuccess = false,
                            firmwareVersion = probeResult.firmwareVersion,
                            detectedProtocol = null,
                            batteryVoltage = probeResult.voltage,
                            capabilities = probeResult.capabilities,
                            quality = probeResult.quality,
                            errorMessage = "Vehicle ECU did not respond to 0100 (${err.messageEn})"
                        )
                    }
                    init0100 is AdapterResponse.Success -> {
                        val pid00 = PidCodec.cleanResponseHex(init0100.rawResponse)
                        val validResponse = PidCodec.verifyResponseHeader(pid00, 0x41, 0x00) && pid00.size >= 6
                        if (!validResponse) {
                            _state.value = ConnectionState.Failure(
                                stage = "ECU_HANDSHAKE",
                                humanMessageFa = "پاسخٔ ECU برای 0100 معتبر نیست",
                                technicalMessage = "0100 response was not a valid Mode 01 PID 00 frame: ${init0100.rawResponse}",
                                isRecoverable = false,
                                rawResponse = init0100.rawResponse
                            )
                            return AdapterInitializationResult(
                                isSuccess = false,
                                firmwareVersion = probeResult.firmwareVersion,
                                detectedProtocol = null,
                                batteryVoltage = probeResult.voltage,
                                capabilities = probeResult.capabilities,
                                quality = probeResult.quality,
                                errorMessage = "Vehicle ECU 0100 response was malformed"
                            )
                        }
                    }
                }

                // 9. Read back detected protocol using ATDPN
                val dpnResp = executeInternal("ATDPN", 1000L)
                if (dpnResp is AdapterResponse.Success && dpnResp.cleanedLines.isNotEmpty()) {
                    val protoNum = dpnResp.cleanedLines.first().removePrefix("A").trim()
                    _currentProtocol = mapElmProtocolNumber(protoNum)
                }

                _state.value = ConnectionState.Connected(
                    deviceName = probeResult.deviceDescription.ifEmpty { "OBD-II Dongle" },
                    adapterType = probeResult.quality.name,
                    protocolName = _currentProtocol?.displayName ?: "OBD-II Standard",
                    elmVersion = probeResult.firmwareVersion,
                    batteryVoltage = probeResult.voltage
                )

                AdapterInitializationResult(
                    isSuccess = true,
                    firmwareVersion = probeResult.firmwareVersion,
                    detectedProtocol = _currentProtocol,
                    batteryVoltage = probeResult.voltage,
                    capabilities = probeResult.capabilities,
                    quality = probeResult.quality
                )

            } catch (e: Exception) {
                _state.value = ConnectionState.Failure(
                    stage = "INITIALIZATION",
                    humanMessageFa = "خطا در آماده‌سازی آداپتور عیب‌یابی",
                    technicalMessage = e.message ?: "Initialization exception",
                    isRecoverable = true
                )
                AdapterInitializationResult(
                    isSuccess = false,
                    firmwareVersion = "Unknown",
                    detectedProtocol = null,
                    batteryVoltage = 0.0f,
                    capabilities = com.sarani.carobd.core.model.AdapterCapabilities(),
                    quality = com.sarani.carobd.core.model.AdapterQuality.COMPATIBLE,
                    errorMessage = e.message
                )
            }
        }

    override suspend fun executeCommand(command: String, timeoutMs: Long): AdapterResponse =
        commandMutex.withLock {
            executeInternal(command, timeoutMs)
        }

    override suspend fun setHeader(headerHex: String): Boolean =
        commandMutex.withLock {
            val resp = executeInternal("ATSH $headerHex", 1000L)
            resp is AdapterResponse.Success
        }

    override suspend fun selectProtocol(protocol: ProtocolStandard): Boolean =
        commandMutex.withLock {
            val cmd = getElmProtocolCommand(protocol)
            val resp = executeInternal(cmd, 1500L)
            if (resp is AdapterResponse.Success) {
                _currentProtocol = protocol
                true
            } else {
                false
            }
        }

    override suspend fun disconnect() {
        commandMutex.withLock {
            try {
                readerJob?.cancel()
                transport.disconnect()
            } catch (_: Exception) {}
            _state.value = ConnectionState.Disconnected
        }
    }

    private suspend fun executeInternal(command: String, timeoutMs: Long): AdapterResponse {
        val startTime = System.currentTimeMillis()
        drainPendingBuffer()

        val outgoing = (command.trim() + "\r").toByteArray(Charsets.US_ASCII)
        try {
            transport.write(outgoing)
        } catch (e: Exception) {
            return AdapterResponse.Error(
                AdapterError.TransportFault(e.message ?: "Write failure"),
                "",
                System.currentTimeMillis() - startTime
            )
        }

        val sb = StringBuilder()
        val receivedPrompt = withTimeoutOrNull(timeoutMs) {
            while (true) {
                val c = incomingCharsChannel.receive()
                sb.append(c)
                if (c == Elm327BufferParser.PROMPT_CHAR) {
                    return@withTimeoutOrNull true
                }
            }
            false
        }

        val elapsed = System.currentTimeMillis() - startTime
        return if (receivedPrompt == true) {
            Elm327BufferParser.parseBuffer(sb.toString(), command, elapsed)
        } else {
            AdapterResponse.Error(
                AdapterError.Timeout(timeoutMs),
                sb.toString(),
                elapsed
            )
        }
    }

    private fun drainPendingBuffer() {
        while (true) {
            val item = incomingCharsChannel.tryReceive().getOrNull() ?: break
        }
    }

    private fun getElmProtocolCommand(standard: ProtocolStandard): String {
        return when (standard) {
            ProtocolStandard.SAE_J1850_PWM -> "ATSP1"
            ProtocolStandard.SAE_J1850_VPW -> "ATSP2"
            ProtocolStandard.ISO_9141_2 -> "ATSP3"
            ProtocolStandard.ISO_14230_4_KWP_5BAUD -> "ATSP4"
            ProtocolStandard.ISO_14230_4_KWP_FAST -> "ATSP5"
            ProtocolStandard.ISO_15765_4_CAN_11_500 -> "ATSP6"
            ProtocolStandard.ISO_15765_4_CAN_29_500 -> "ATSP7"
            ProtocolStandard.ISO_15765_4_CAN_11_250 -> "ATSP8"
            ProtocolStandard.ISO_15765_4_CAN_29_250 -> "ATSP9"
            ProtocolStandard.SAE_J1939_CAN -> "ATSPB"
            else -> "ATSP0"
        }
    }

    private fun mapElmProtocolNumber(numStr: String): ProtocolStandard {
        return when (numStr) {
            "1" -> ProtocolStandard.SAE_J1850_PWM
            "2" -> ProtocolStandard.SAE_J1850_VPW
            "3" -> ProtocolStandard.ISO_9141_2
            "4" -> ProtocolStandard.ISO_14230_4_KWP_5BAUD
            "5" -> ProtocolStandard.ISO_14230_4_KWP_FAST
            "6" -> ProtocolStandard.ISO_15765_4_CAN_11_500
            "7" -> ProtocolStandard.ISO_15765_4_CAN_29_500
            "8" -> ProtocolStandard.ISO_15765_4_CAN_11_250
            "9" -> ProtocolStandard.ISO_15765_4_CAN_29_250
            "B", "b" -> ProtocolStandard.SAE_J1939_CAN
            else -> ProtocolStandard.ISO_15765_4_CAN_11_500
        }
    }
}
