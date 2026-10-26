package com.sarani.carobd.core.adapter.stn

import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.adapter.elm327.Elm327Adapter
import com.sarani.carobd.core.adapter.transport.ObdTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Enhanced adapter implementation for Scantool.net STN11xx / STN21xx / OBDLink MX+ chips.
 * Supports extended STN commands, hardware CAN filtering, and higher baud rates.
 */
class StnAdapter(
    transport: ObdTransport,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : Elm327Adapter(transport, scope) {

    /**
     * Resets hardware CAN filters on STN chip.
     */
    suspend fun resetCanFilters(): Boolean {
        val resp = executeCommand("STPRS", 1000L)
        return resp is AdapterResponse.Success
    }

    /**
     * Adds hardware CAN acceptance filter on STN chip (passes only matching IDs).
     */
    suspend fun addCanFilter(filterHex: String, maskHex: String): Boolean {
        val resp = executeCommand("STFAP $filterHex,$maskHex", 1000L)
        return resp is AdapterResponse.Success
    }

    /**
     * Enables high speed UART baudrate on supported STN adapters.
     */
    suspend fun setHighSpeedBaud(baudrate: Int): Boolean {
        val resp = executeCommand("STPBR $baudrate", 1000L)
        return resp is AdapterResponse.Success
    }

    /**
     * Transmits multi-frame payload directly using STPX.
     */
    suspend fun transmitMultiFrame(headerHex: String, hexData: String): AdapterResponse {
        return executeCommand("STPX h:$headerHex,d:$hexData", 3000L)
    }
}
