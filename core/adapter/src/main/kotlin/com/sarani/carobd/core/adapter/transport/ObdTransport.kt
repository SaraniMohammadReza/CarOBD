package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.flow.Flow

/**
 * Low-level bidirectional transport abstraction.
 * Decouples OBD adapter logic from the underlying communication mechanism
 * (Bluetooth Classic RFCOMM, BLE GATT, Wi-Fi TCP Socket, USB FTDI/CH340/CP2102, or Simulator).
 */
interface ObdTransport {
    val transportType: TransportType
    val isConnected: Boolean

    /**
     * Flow of incoming raw bytes received from the hardware channel.
     */
    val incomingBytes: Flow<ByteArray>

    /**
     * Establishes the low-level communication channel.
     * Throws [TransportException] if connection fails.
     */
    suspend fun connect()

    /**
     * Closes the transport channel and releases hardware resources.
     */
    suspend fun disconnect()

    /**
     * Transmits raw bytes to the adapter over the channel.
     */
    suspend fun write(data: ByteArray)
}

open class TransportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class ConnectionFailedException(message: String, cause: Throwable? = null) : TransportException(message, cause)
class TransportTimeoutException(message: String) : TransportException(message)
class TransportDisconnectedException(message: String = "Transport disconnected unexpectedly") : TransportException(message)
