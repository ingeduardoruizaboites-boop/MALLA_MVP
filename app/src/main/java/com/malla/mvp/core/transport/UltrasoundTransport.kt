package com.malla.mvp.core.transport

class UltrasoundTransport : MeshTransport() {
    override val level = com.malla.mvp.core.engine.MeshLevel.ULTRASOUND
    override val isAvailable = false
    override val estimatedBatteryDrainPerMinute = 8
    override suspend fun initialize(): Boolean = false
    override suspend fun dispose() {}
    override suspend fun discoverNearbyNodes(): List<String> = emptyList()
}
