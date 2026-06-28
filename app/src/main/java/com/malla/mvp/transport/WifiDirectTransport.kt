package com.malla.mvp.transport

import com.malla.mvp.core.crypto.ICryptoEngine
import com.malla.mvp.core.data.ISessionStore
import com.malla.mvp.core.identity.IIdentityManager
import com.malla.mvp.core.transport.ITransport
import com.malla.mvp.core.transport.TransportState
import com.malla.mvp.core.util.IAppContext
import com.malla.mvp.core.wifi.IWifiDirectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class WifiDirectTransport(
    appContext: IAppContext,
    wifiManager: IWifiDirectManager,
    crypto: ICryptoEngine,
    identityManager: IIdentityManager,
    sessionStore: ISessionStore? = null
) : ITransport {
    override val type = "WIFI_DIRECT"
    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: Flow<TransportState> = _state
    override val isAuthenticated: Flow<Boolean> = MutableStateFlow(false)
    override suspend fun connect(address: String) {}
    override suspend fun send(payload: ByteArray) {}
    override suspend fun disconnect() {}
}
