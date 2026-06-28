package com.malla.mvp.core.transport

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

class BleTransport(private val context: Context) : MeshTransport() {
    override val level = com.malla.mvp.core.engine.MeshLevel.BLE
    override val isAvailable = true
    override suspend fun initialize(): Boolean = true
    override suspend fun dispose() {}
    override suspend fun discoverNearbyNodes(): List<String> = emptyList()
}
