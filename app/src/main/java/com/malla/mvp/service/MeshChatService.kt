package com.malla.mvp.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.malla.mvp.network.BleManager
import com.malla.mvp.network.DiscoveryService
import com.malla.mvp.network.WifiDirectManager

class MeshChatService : Service() {

    companion object {
        private const val TAG = "MeshChatService"
        private const val SCAN_DURATION_MS = 30_000L      // 30 segundos de escaneo
        private const val PAUSE_DURATION_MS = 5 * 60_000L // 5 minutos de pausa
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false

    // Runnable que inicia los escaneos y programa la pausa
    private val startScanRunnable: Runnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Iniciando escaneo de 30s")
            isScanning = true
            WifiDirectManager.start(this@MeshChatService)
            BleManager.start(this@MeshChatService)
            DiscoveryService.start(this@MeshChatService)

            // Programar la detención del escaneo después de SCAN_DURATION_MS
            handler.postDelayed(stopScanRunnable, SCAN_DURATION_MS)
        }
    }

    // Runnable que detiene los escaneos y programa el próximo inicio
    private val stopScanRunnable: Runnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Pausando escaneo por 5 minutos")
            WifiDirectManager.stop()
            BleManager.stop()
            DiscoveryService.stop()
            isScanning = false

            // Programar el próximo escaneo después de PAUSE_DURATION_MS
            handler.postDelayed(startScanRunnable, PAUSE_DURATION_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio mesh creado – iniciando ciclo de escaneo intermitente y advertising continuo")
        // Iniciar el primer escaneo inmediatamente
        handler.post(startScanRunnable)
        // Iniciar advertising BLE para que el dispositivo siempre sea visible
        BleManager.startAdvertising()
    }

    override fun onDestroy() {
        // Cancelar todos los callbacks pendientes
        handler.removeCallbacks(startScanRunnable)
        handler.removeCallbacks(stopScanRunnable)

        // Detener cualquier escaneo en curso
        if (isScanning) {
            WifiDirectManager.stop()
            BleManager.stop()
            DiscoveryService.stop()
        }

        // Detener el advertising BLE
        BleManager.stopAdvertising()

        Log.d(TAG, "Servicio mesh destruido – escaneos y advertising detenidos")
        super.onDestroy()
    }
}
