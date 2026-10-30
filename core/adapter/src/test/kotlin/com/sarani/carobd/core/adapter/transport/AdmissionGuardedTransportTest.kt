package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AdmissionGuardedTransportTest {
    private class Link : ObdTransport {
        override val transportType = TransportType.BLUETOOTH_LE
        override var isConnected = false
        override val incomingBytes = emptyFlow<ByteArray>()
        var connects = 0; var closes = 0; var writes = 0
        var duringConnect: () -> Unit = {}
        override suspend fun connect() { connects++; isConnected = true; duringConnect() }
        override suspend fun disconnect() { closes++; isConnected = false }
        override suspend fun write(data: ByteArray) { writes++ }
    }
    @Test fun initialDenialNeverOpensLinkOrWrites() = runTest {
        val link = Link(); val guard = AdmissionGuardedTransport(link) { throw SecurityException("Denied") }
        try { guard.connect(); fail("Expected denial") } catch (_: SecurityException) {}
        assertEquals(0, link.connects); assertEquals(0, link.writes); assertFalse(guard.isConnected)
        guard.disconnect(); assertEquals(1, link.closes)
    }
    @Test fun revocationBeforeWriteClosesWithoutSendingAndRegrantCannotReviveGeneration() = runTest {
        val link = Link(); var allowed = true
        val guard = AdmissionGuardedTransport(link) { if (!allowed) throw SecurityException("Revoked") }
        guard.connect(); guard.write(byteArrayOf(1)); assertEquals(1, link.writes)
        allowed = false
        try { guard.write(byteArrayOf(2)); fail("Expected denial") } catch (_: SecurityException) {}
        assertEquals(1, link.writes); assertEquals(1, link.closes)
        allowed = true
        try { guard.connect(); fail("Retired generation cannot reconnect") } catch (_: TransportDisconnectedException) {}
        assertFalse(guard.isConnected); assertEquals(1, link.connects)
    }
    @Test fun passiveLinkCheckDetectsRevocationAndCleanupNeedsNoPermission() = runTest {
        val link = Link(); var allowed = true
        val guard = AdmissionGuardedTransport(link) { if (!allowed) throw SecurityException("Revoked") }
        guard.connect(); assertTrue(guard.isConnected)
        allowed = false; assertFalse(guard.isConnected)
        guard.disconnect(); assertEquals(1, link.closes); assertFalse(link.isConnected)
        assertEquals(0, link.writes)
    }
    @Test fun revocationDuringConnectClosesTheNewLinkBeforeReturning() = runTest {
        val link = Link(); var allowed = true
        link.duringConnect = { allowed = false }
        val guard = AdmissionGuardedTransport(link) { if (!allowed) throw SecurityException("Revoked") }
        try { guard.connect(); fail("Expected denial") } catch (_: SecurityException) {}
        assertFalse(link.isConnected); assertFalse(guard.isConnected); assertEquals(1, link.closes)
    }
    @Test fun cancellationStillClosesWithoutExtraWrite() = runTest {
        val link = Link(); link.duringConnect = { throw CancellationException("Canceled") }
        val guard = AdmissionGuardedTransport(link) {}
        try { guard.connect(); fail("Expected cancellation") } catch (_: CancellationException) {}
        assertEquals(1, link.closes); assertEquals(0, link.writes)
    }
}
