package com.malla.mvp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.malla.mvp.core.engine.LogBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ConnectivityMonitor {
    private const val TAG = "ConnectivityMonitor"
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    fun start(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Internet disponible")
                LogBuffer.add("NET", "Internet disponible – mesh inactivo")
                _isOnline.value = true
            }
            override fun onLost(network: Network) {
                Log.d(TAG, "Internet perdido – activando modo mesh")
                LogBuffer.add("NET", "Internet perdido – modo mesh activado")
                _isOnline.value = false
            }
        })
        // Estado inicial
        val currentNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(currentNetwork)
        _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        LogBuffer.add("NET", if (_isOnline.value) "Estado inicial: ONLINE" else "Estado inicial: MESH")
    }
}
