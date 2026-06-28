package com.malla.mvp.core.transport

class SmsTransport : MeshTransport() {
    override val level = com.malla.mvp.core.engine.MeshLevel.SMS_BRIDGE
    override val isAvailable = false
    override val estimatedBatteryDrainPerMinute = 3
    override suspend fun initialize(): Boolean = false
    override suspend fun dispose() {}
    override suspend fun discoverNearbyNodes(): List<String> = emptyList()
}
