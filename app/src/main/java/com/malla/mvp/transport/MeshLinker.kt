package com.malla.mvp.transport

import com.malla.mvp.core.transport.ITransport
import com.malla.mvp.core.util.IAppContext
import kotlinx.coroutines.CoroutineScope

class MeshLinker(
    appContext: IAppContext,
    transports: List<ITransport>,
    bleTransport: BleTransport? = null,
    wifiTransport: WifiDirectTransport? = null
) {
    fun startAutoConnect(scope: CoroutineScope) {}
    suspend fun connectToBest(address: String): ITransport? = null
    suspend fun disconnectAll() {}
}
