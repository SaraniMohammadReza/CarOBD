package com.sarani.carobd.core.adapter

import com.sarani.carobd.core.adapter.command.AdapterInitializationResult
import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.model.AdapterCapabilities
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ProtocolStandard
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level automotive diagnostic adapter interface.
 * Controls ELM327 / STN / OBDLink hardware.
 */
interface ObdAdapter {
    /** Current connection and handshake state */
    val state: StateFlow<ConnectionState>

    /** Current detected protocol */
    val currentProtocol: ProtocolStandard?

    /** Battery voltage measured by adapter */
    val batteryVoltage: Float

    /**
     * Initializes adapter hardware with initialization string sequence (ATZ, ATE0, etc.)
     * and negotiates protocol with the vehicle.
     */
    suspend fun initialize(forcedProtocol: ProtocolStandard? = null): AdapterInitializationResult

    /**
     * Sends a command (AT or OBD hex) and awaits completion marked by prompt '>'.
     */
    suspend fun executeCommand(command: String, timeoutMs: Long = 2000L): AdapterResponse

    /**
     * Sets target CAN header / ECU physical address (e.g. "7E0" or "18DB33F1").
     */
    suspend fun setHeader(headerHex: String): Boolean

    /**
     * Selects communication protocol (e.g. ISO 15765-4 CAN, ISO 14230 KWP, etc.).
     */
    suspend fun selectProtocol(protocol: ProtocolStandard): Boolean

    /**
     * Shuts down communication and releases adapter.
     */
    suspend fun disconnect()
}
