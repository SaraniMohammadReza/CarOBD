package com.sarani.carobd.core.adapter.transport

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/** Rechecks platform admission without adding a command path, reader, or producer.
 * A revoked generation never revives after permission is granted again. The existing
 * session link monitor observes false and owns teardown even when no writes occur. */
class AdmissionGuardedTransport(private val delegate: ObdTransport, private val admit: () -> Unit) : ObdTransport {
    override val transportType get() = delegate.transportType
    override val incomingBytes get() = delegate.incomingBytes
    @Volatile private var retired = false
    @Volatile private var admissionFailure: Exception? = null
    private val closed = AtomicBoolean(false)

    override val isConnected: Boolean get() {
        if (retired || !delegate.isConnected) return false
        try { admit() } catch (e: Exception) {
            admissionFailure = e
            retired = true
            return false
        }
        return !retired && delegate.isConnected
    }

    private fun requireActive() {
        if (retired) throw admissionFailure ?: TransportDisconnectedException("Transport admission generation retired")
    }
    private suspend fun closeDelegate() = withContext(NonCancellable) {
        retired = true
        if (closed.compareAndSet(false, true)) delegate.disconnect()
    }
    override suspend fun connect() {
        try {
            requireActive(); admit()
            delegate.connect()
            requireActive(); admit() // Permission can change while a blocking connect is pending.
        } catch (e: Exception) {
            runCatching { closeDelegate() }
            throw e
        }
    }
    override suspend fun write(data: ByteArray) {
        try {
            requireActive(); admit(); requireActive()
            delegate.write(data)
        } catch (e: Exception) {
            runCatching { closeDelegate() }
            throw e
        }
    }
    override suspend fun disconnect() = closeDelegate()
}
