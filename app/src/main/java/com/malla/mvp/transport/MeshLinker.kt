package com.malla.mvp.transport

class MeshLinker(
    private val transports: List<Any>,
    private val bleTransport: BleTransport? = null,
    private val wifiTransport: WifiDirectTransport? = null
) {
    suspend fun disconnectAll() {}
}
