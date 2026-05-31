package com.malla.mvp.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object WifiDirectManager {
    private const val TAG = "WifiDirectManager"
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private val _peers = MutableStateFlow<List<String>>(emptyList())
    val peers: StateFlow<List<String>> = _peers

    fun start(context: Context) {
        manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(context, Looper.getMainLooper(), null)
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d(TAG, "Descubrimiento WiFi Direct iniciado") }
            override fun onFailure(reason: Int) { Log.e(TAG, "Fallo al iniciar descubrimiento: $reason") }
        })
    }

    fun stop() {
        manager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d(TAG, "Descubrimiento detenido") }
            override fun onFailure(reason: Int) { Log.e(TAG, "Fallo al detener: $reason") }
        })
    }

    fun connectToPeer(address: String) {
        // implementar después
    }
}
