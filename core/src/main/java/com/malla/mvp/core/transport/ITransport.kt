package com.malla.mvp.core.transport

import kotlinx.coroutines.flow.Flow

interface ITransport {
    val type: String
    val state: Flow<TransportState>
    val isAuthenticated: Flow<Boolean>
    suspend fun connect(address: String)
    suspend fun send(payload: ByteArray)
    suspend fun disconnect()
}

sealed class TransportState {
    object Idle : TransportState()
    object Connecting : TransportState()
    object Connected : TransportState()
    object Disconnecting : TransportState()
    data class Error(val message: String) : TransportState()
}
