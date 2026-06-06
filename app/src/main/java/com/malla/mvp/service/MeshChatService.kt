package com.malla.mvp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.malla.mvp.core.engine.PulseManager
import com.malla.mvp.network.GattServerManager
import com.malla.mvp.network.MessageBridge

class MeshChatService : Service() {

    companion object {
        private const val TAG = "MeshChatService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "[SVC:LIFE] Servicio mesh CREADO – iniciando PulseManager, GATT, MessageBridge")

        // Iniciar el orquestador central (maneja BLE, intervalos, decisiones)
        PulseManager.initialize(this)

        // Servidor GATT (expone IP local)
        GattServerManager.start(this)

        // Puente de mensajes (red -> Room)
        MessageBridge.start(this)
    }

    override fun onDestroy() {
        Log.d(TAG, "[SVC:LIFE] Servicio mesh DESTRUIDO – apagando todos los servicios")

        // Apagar el orquestador central
        PulseManager.shutdown()

        // Servidor GATT
        GattServerManager.stop()

        // Puente de mensajes
        MessageBridge.stop()

        super.onDestroy()
    }
}
