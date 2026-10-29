package com.sarani.carobd.data.session

import com.sarani.carobd.core.adapter.ObdAdapter
import com.sarani.carobd.core.adapter.command.*
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticSessionTest {
    private class Transport : ObdTransport {
        override val transportType = TransportType.WIFI_TCP
        override var isConnected = true
        override val incomingBytes = emptyFlow<ByteArray>()
        override suspend fun connect() { isConnected = true }
        override suspend fun disconnect() { isConnected = false }
        override suspend fun write(data: ByteArray) {}
    }
    private class Adapter : ObdAdapter {
        override val state = MutableStateFlow<ConnectionState>(ConnectionState.Connected("fake", "fake", "CAN", "fake", 0f))
        override val currentProtocol = ProtocolStandard.ISO_15765_4_CAN_11_500
        override val batteryVoltage = 0f
        val calls = mutableListOf<String>()
        override suspend fun initialize(forcedProtocol: ProtocolStandard?) = error("unused")
        override suspend fun executeCommand(command: String, timeoutMs: Long): AdapterResponse {
            calls.add(command)
            val raw = if (command == "0100") "41 00 FF FF FF 00" else "NO DATA"
            return AdapterResponse.Success(raw, listOf(raw), 1)
        }
        override suspend fun setHeader(headerHex: String) = false
        override suspend fun selectProtocol(protocol: ProtocolStandard) = false
        override suspend fun disconnect() { state.value = ConnectionState.Disconnected }
    }
    @Test fun repository_repeatedStartTelemetryIsIdempotent() = runTest {
        val transport = Transport(); val adapter = Adapter()
        var samples = 0
        val session = DiagnosticSession(transport, backgroundScope, {}, { samples++ }, {}, adapterFactory = { _, _ -> adapter })
        repeat(10) { session.startTelemetry(MutableStateFlow(TelemetryPollMode.NORMAL)) }
        runCurrent()
        assertEquals(1, adapter.calls.count { it == "0100" })
        assertEquals(8, adapter.calls.size)
        session.close()
        val count = adapter.calls.size; val emitted = samples
        advanceTimeBy(5000)
        assertEquals(count, adapter.calls.size); assertEquals(emitted, samples)
    }
    @Test fun transportLossInvalidatesSession() = runTest {
        val transport = Transport(); val adapter = Adapter()
        var loss = 0
        val session = DiagnosticSession(transport, backgroundScope, {}, {}, { loss++ }, adapterFactory = { _, _ -> adapter })
        session.startTelemetry(MutableStateFlow(TelemetryPollMode.NORMAL)); runCurrent()
        transport.isConnected = false
        advanceTimeBy(150); runCurrent()
        assertFalse(session.isOpen); assertEquals(1, loss)
        val calls = adapter.calls.size
        advanceTimeBy(5000)
        assertEquals(calls, adapter.calls.size)
        session.close()
    }
    @Test fun disconnectCancelsOwnedRequestsAndRejectsOldEmissions() = runTest {
        val transport = Transport(); val adapter = Adapter()
        val session = DiagnosticSession(transport, backgroundScope, {}, {}, {}, adapterFactory = { _, _ -> adapter })
        val request = launch { session.execute { awaitCancellation() } }
        runCurrent(); session.close(); runCurrent()
        assertTrue(request.isCancelled)
        assertFalse(session.isOpen)
        assertFalse(transport.isConnected)
    }
    @Test fun lossBeforeFirstWatchTickStillInvalidatesConnectedAdapter() = runTest {
        val transport = Transport(); val adapter = Adapter()
        transport.isConnected = false
        var loss = 0
        val session = DiagnosticSession(transport, backgroundScope, {}, {}, { loss++ }, adapterFactory = { _, _ -> adapter })
        runCurrent(); advanceTimeBy(200); runCurrent()
        assertFalse(session.isOpen)
        assertEquals(1, loss)
        session.close()
    }

}
