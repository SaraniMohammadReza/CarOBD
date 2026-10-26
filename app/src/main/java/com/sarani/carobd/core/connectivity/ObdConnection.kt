package com.sarani.carobd.core.connectivity

import com.sarani.carobd.core.model.ConnectionState
import kotlinx.coroutines.flow.StateFlow

interface ObdConnection {
    val connectionState: StateFlow<ConnectionState>

    suspend fun connect(target: String): Result<Unit>
    suspend fun disconnect()
    suspend fun sendCommand(command: String): Result<String>
    fun isConnected(): Boolean
}
