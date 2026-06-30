package com.malla.mvp.network

import com.malla.mvp.core.network.MeshMessage
import com.malla.mvp.core.transport.SmsTransport
import com.malla.mvp.transport.BleTransport
import com.malla.mvp.transport.WifiDirectTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object MeshMessageHub {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _messages = MutableSharedFlow<MeshMessage>(replay = 10)
    val messages: SharedFlow<MeshMessage> = _messages.asSharedFlow()

    fun start(
        ble: BleTransport?,
        wifi: WifiDirectTransport?,
        internet: Any? = null,
        sms: SmsTransport? = null,
        ultrasound: Any? = null
    ) {
        // Escuchar BLE
        // Escuchar Wi‑Fi Direct
        // Escuchar SMS
        // Escuchar Internet
        // Escuchar Ultrasonido
    }

    fun stop() {
        scope.cancel()
    }
}
