package com.malla.mvp.core.transport

class NfcTransport : MeshTransport() {
    override val level = com.malla.mvp.core.engine.MeshLevel.NFC
    override val isAvailable = false
    override val estimatedBatteryDrainPerMinute = 1
    override suspend fun initialize(): Boolean = false
    override suspend fun dispose() {}
    override suspend fun discoverNearbyNodes(): List<String> = emptyList()
}
