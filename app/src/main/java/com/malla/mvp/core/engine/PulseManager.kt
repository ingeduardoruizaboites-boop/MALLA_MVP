package com.malla.mvp.core.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.malla.mvp.core.transport.BleTransport
import com.malla.mvp.core.transport.MeshTransport
import com.malla.mvp.core.transport.NfcTransport
import com.malla.mvp.core.transport.SmsTransport
import com.malla.mvp.core.transport.UltrasoundTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object PulseManager {
    private const val TAG = "PulseManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val transports = mutableMapOf<MeshLevel, MeshTransport>()
    private var activePrimary: MeshTransport? = null
    private var activeSecondary: MeshTransport? = null
    private var pulseRunnable: Runnable? = null
    private var currentIntervalSeconds = 60
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) {
            Log.w(TAG, "[PM:LIFE] Ya estaba inicializado")
            return
        }
        Log.d(TAG, "[PM:LIFE] Inicializando PulseManager")

        transports[MeshLevel.BLE] = BleTransport(context)
        transports[MeshLevel.NFC] = NfcTransport()
        transports[MeshLevel.ULTRASOUND] = UltrasoundTransport()
        transports[MeshLevel.SMS_BRIDGE] = SmsTransport()

        scope.launch {
            val bleTransport = transports[MeshLevel.BLE]
            if (bleTransport != null && bleTransport.isAvailable) {
                bleTransport.initialize()
                activePrimary = bleTransport
                Log.d(TAG, "[PM:INIT] BLE inicializado como transporte primario")
            }
        }

        scope.launch {
            DeviceStateMonitor.state.collect { state ->
                val decision = DecisionEngine.decide(state)
                Log.d(TAG, "[PM:DECIDE] ${decision.reasoning} | Primario: ${decision.primaryTransport} | Intervalo: ${decision.scanIntervalSeconds}s")
                applyDecision(decision)
            }
        }

        isInitialized = true
    }

    fun shutdown() {
        Log.d(TAG, "[PM:LIFE] Apagando PulseManager")
        pulseRunnable?.let { handler.removeCallbacks(it) }
        scope.launch {
            activePrimary?.dispose()
            activeSecondary?.dispose()
        }
        transports.clear()
        isInitialized = false
    }

    private fun applyDecision(decision: CommunicationDecision) {
        if (decision.scanIntervalSeconds != currentIntervalSeconds) {
            currentIntervalSeconds = decision.scanIntervalSeconds
            restartPulseCycle()
        }

        val newPrimary = transports[decision.primaryTransport]
        if (newPrimary != null && activePrimary?.level != newPrimary.level) {
            scope.launch {
                activePrimary?.dispose()
                if (newPrimary.isAvailable) {
                    newPrimary.initialize()
                    activePrimary = newPrimary
                    Log.d(TAG, "[PM:TRANS] Primario cambiado a: ${newPrimary.level}")
                }
            }
        }

        val newSecondary = decision.secondaryTransport?.let { transports[it] }
        if (newSecondary != null && activeSecondary?.level != newSecondary.level) {
            scope.launch {
                activeSecondary?.dispose()
                if (newSecondary.isAvailable) {
                    newSecondary.initialize()
                    activeSecondary = newSecondary
                    Log.d(TAG, "[PM:TRANS] Secundario cambiado a: ${newSecondary.level}")
                }
            }
        } else if (decision.secondaryTransport == null && activeSecondary != null) {
            scope.launch {
                activeSecondary?.dispose()
                activeSecondary = null
                Log.d(TAG, "[PM:TRANS] Secundario desactivado")
            }
        }
    }

    private fun restartPulseCycle() {
        pulseRunnable?.let { handler.removeCallbacks(it) }
        val runnable = object : Runnable {
            override fun run() {
                executePulse()
                handler.postDelayed(this, currentIntervalSeconds * 1000L)
            }
        }
        pulseRunnable = runnable
        handler.post(runnable)
        Log.d(TAG, "[PM:CYCLE] Ciclo de pulsos reiniciado: cada ${currentIntervalSeconds}s")
    }

    private fun executePulse() {
        scope.launch {
            try {
                val nodes = activePrimary?.discoverNearbyNodes() ?: emptyList()
                Log.d(TAG, "[PM:PULSE] Nodos descubiertos: ${nodes.size}")

                val state = DeviceStateMonitor.state.value
                when (state.deviceLoad) {
                    DeviceLoad.HEAVY -> Log.d(TAG, "[PM:MEM] Carga pesada - reduciendo buffers")
                    DeviceLoad.CRITICAL -> {
                        Log.w(TAG, "[PM:MEM] Carga crítica - limpieza de emergencia")
                        emergencyMemoryCleanup()
                    }
                    else -> { /* OK */ }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[PM:ERR] Error en pulso: ${e.message}", e)
            }
        }
    }

    private suspend fun emergencyMemoryCleanup() {
        System.gc()
        Log.w(TAG, "[PM:MEM] Limpieza de emergencia ejecutada")
    }
}
