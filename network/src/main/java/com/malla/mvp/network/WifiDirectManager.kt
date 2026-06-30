package com.malla.mvp.network

import com.malla.mvp.core.wifi.IWifiDirectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object WifiDirectManager : IWifiDirectManager {
    private val _peers = MutableStateFlow<List<String>>(emptyList())
    override val peers: StateFlow<List<String>> = _peers

    override fun start(context: android.content.Context) {
        // Inicializar Wi-Fi Direct
    }

    override fun stop() {
        // Detener Wi-Fi Direct
    }

    override fun connectToPeer(address: String) {
        // Conectar a un peer
    }
}
