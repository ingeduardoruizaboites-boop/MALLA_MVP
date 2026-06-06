package com.malla.mvp.core.engine

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object EmergencyMode {
    private const val TAG = "EmergencyMode"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private var deactivationJob: Job? = null

    fun activate(durationMs: Long = 5 * 60 * 1000L) {
        if (_isActive.value) {
            Log.d(TAG, "[EMERGENCY] Ya estaba activo, renovando temporizador")
            deactivationJob?.cancel()
        } else {
            _isActive.value = true
            Log.d(TAG, "[EMERGENCY] Modo emergencia ACTIVADO")
        }
        deactivationJob = scope.launch {
            delay(durationMs)
            deactivate()
        }
    }

    fun deactivate() {
        _isActive.value = false
        deactivationJob?.cancel()
        Log.d(TAG, "[EMERGENCY] Modo emergencia DESACTIVADO")
    }
}
