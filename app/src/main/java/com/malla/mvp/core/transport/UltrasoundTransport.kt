package com.malla.mvp.core.transport

import com.malla.mvp.core.engine.MeshLevel

class UltrasoundTransport : MeshTransport() {
    override val level = MeshLevel.ULTRASOUND
    override val isAvailable = false
    override val estimatedBatteryDrainPerMinute = 8
    override suspend fun initialize(): Boolean = false
    override suspend fun dispose() {}
    override suspend fun discoverNearbyNodes(): List<String> = emptyList()
}
