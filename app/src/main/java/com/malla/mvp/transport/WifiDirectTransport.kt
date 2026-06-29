package com.malla.mvp.transport

import com.malla.mvp.core.transport.ITransport
import com.malla.mvp.core.transport.TransportState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class WifiDirectTransport : ITransport {
    override val type = "WIFI_DIRECT"
    override val state: Flow<TransportState> = MutableStateFlow(TransportState.Idle)
    override val isAuthenticated: Flow<Boolean> = MutableStateFlow(false)
    override suspend fun connect(address: String) {}
    override suspend fun send(payload: ByteArray) {}
    override suspend fun disconnect() {}
    fun startDiscovery() {}
    fun stopDiscovery() {}
}
