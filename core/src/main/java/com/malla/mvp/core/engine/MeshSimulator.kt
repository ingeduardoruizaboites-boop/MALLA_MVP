package com.malla.mvp.core.engine

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object MeshSimulator {
    private const val TAG = "MeshSimulator"
    private val _isSimulated = MutableStateFlow(false)
    val isSimulated: StateFlow<Boolean> = _isSimulated

    fun toggle(active: Boolean) {
        _isSimulated.value = active
        Log.d(TAG, if (active) "[SIM] Mesh SIMULADO (sin internet)" else "[SIM] Mesh NORMAL (internet real)")
    }
}
