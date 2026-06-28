package com.malla.mvp.core.wifi

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface IWifiDirectManager {
    val peers: StateFlow<List<String>>
    fun start(context: Context)
    fun stop()
    fun connectToPeer(address: String)
}
