package com.malla.mvp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.core.engine.PulseManager
import com.malla.mvp.network.GattServerManager
import com.malla.mvp.network.MeshConnector
import com.malla.mvp.network.MeshMessageHandler
import com.malla.mvp.network.MessageBridge
import com.malla.mvp.network.NetworkService

class MeshChatService : Service() {

    companion object {
        private const val TAG = "MeshChatService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "[SVC:LIFE] Servicio mesh CREADO – iniciando stack completo")
        LogBuffer.add("SVC", "Iniciando stack mesh")

        // Servidor TCP (para recibir conexiones entrantes)
        NetworkService.startServer()
        LogBuffer.add("SVC", "NetworkService iniciado")

        // Manejador de mensajes entrantes (anti-replay, bloom, room)
        MeshMessageHandler.start(this)
        LogBuffer.add("SVC", "MeshMessageHandler iniciado")

        // Orquestador central (BLE, intervalos, decisiones)
        PulseManager.initialize(this)
        LogBuffer.add("SVC", "PulseManager iniciado")

        // Servidor GATT (expone IP local)
        GattServerManager.start(this)
        LogBuffer.add("SVC", "GattServerManager iniciado")

        // Conector automático (observa BLE y conecta TCP)
        MeshConnector.start()
        LogBuffer.add("SVC", "MeshConnector iniciado")

        // Puente de mensajes (red -> Room)
        MessageBridge.start(this)
        LogBuffer.add("SVC", "MessageBridge iniciado")
    }

    override fun onDestroy() {
        Log.d(TAG, "[SVC:LIFE] Servicio mesh DESTRUIDO – apagando stack")
        MeshConnector.stop()
        MessageBridge.stop()
        GattServerManager.stop()
        PulseManager.shutdown()
        MeshMessageHandler.stop()
        NetworkService.stopServer()
        super.onDestroy()
    }
}
