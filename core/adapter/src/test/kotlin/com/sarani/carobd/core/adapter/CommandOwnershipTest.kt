package com.sarani.carobd.core.adapter

import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.adapter.elm327.Elm327Adapter
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommandOwnershipTest {
    private class Physical(private val scope: CoroutineScope, val blockWrite: Boolean = false) : ObdTransport {
        override val transportType = TransportType.WIFI_TCP
        override var isConnected = true
        val inbound = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)
        override val incomingBytes = inbound.asSharedFlow()
        val writes = mutableListOf<String>()
        override suspend fun connect() { isConnected = true }
        override suspend fun disconnect() { isConnected = false }
        override suspend fun write(data: ByteArray) {
            val cmd = data.toString(Charsets.US_ASCII).trim()
            writes.add(cmd) // Count every attempt, including after a drop.
            if (blockWrite) awaitCancellation()
            scope.launch {
                if (cmd == "010C") delay(150)
                inbound.emit((if (cmd == "010C") "41 0C 1A F8\r>" else "41 0D 55\r>").toByteArray())
            }
        }
    }
    private fun canceled(blockWrite: Boolean = false) = runTest {
        val t = Physical(backgroundScope, blockWrite)
        val a = Elm327Adapter(t, backgroundScope)
        runCurrent()
        val owner = launch { a.executeCommand("010C", 1000) }
        runCurrent()
        advanceTimeBy(20)
        owner.cancelAndJoin()
        assertFalse(t.isConnected)
        assertTrue(a.executeCommand("010D") is AdapterResponse.Error)
        advanceTimeBy(200)
        assertEquals(listOf("010C"), t.writes)
        a.disconnect()
    }
    @Test fun adapter_canceledCommandCannotCompleteNextCommand() = canceled()
    @Test fun adapter_cancelDuringReceive_invalidatesOwnership() = canceled()
    @Test fun adapter_cancelDuringWriteInvalidatesOwnership() = canceled(true)
    @Test fun adapter_lateResponseAfterCancelDoesNotCompleteNext() = canceled()
    @Test fun adapter_timeoutQuarantineStillWorks() = runTest {
        val t = Physical(backgroundScope)
        val a = Elm327Adapter(t, backgroundScope)
        runCurrent()
        assertTrue(a.executeCommand("010C", 100) is AdapterResponse.Error)
        val response = a.executeCommand("010D", 1000) as AdapterResponse.Success
        assertEquals("41 0D 55", response.rawResponse)
        assertEquals(listOf("010C", "010D"), t.writes)
        a.disconnect()
    }
    @Test fun adapter_reconnectUsesNewGeneration() = runTest {
        val old = Physical(backgroundScope, true)
        val a = Elm327Adapter(old, backgroundScope)
        runCurrent()
        val job = launch { a.executeCommand("010C") }
        runCurrent(); job.cancelAndJoin()
        val fresh = Physical(backgroundScope)
        val b = Elm327Adapter(fresh, backgroundScope)
        runCurrent()
        assertEquals("41 0D 55", (b.executeCommand("010D") as AdapterResponse.Success).rawResponse)
        assertTrue(a.executeCommand("010D") is AdapterResponse.Error)
        a.disconnect(); b.disconnect()
    }
}
