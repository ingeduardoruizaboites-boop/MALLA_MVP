package com.malla.mvp.transport

import com.malla.mvp.core.transport.ITransport
import com.malla.mvp.core.transport.TransportState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TcpDirectTransport : ITransport {
    override val type = "TCP_DIRECT"
    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: Flow<TransportState> = _state
    override val isAuthenticated: Flow<Boolean> = MutableStateFlow(false)
    override suspend fun connect(address: String) {}
    override suspend fun send(payload: ByteArray) {}
    override suspend fun disconnect() {}
}
