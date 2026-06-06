package com.malla.mvp.core.transport

import com.malla.mvp.core.engine.MeshLevel

class NfcTransport : MeshTransport() {
    override val level = MeshLevel.NFC
    override val isAvailable = false
    override val estimatedBatteryDrainPerMinute = 1
    override suspend fun initialize(): Boolean = false
    override suspend fun dispose() {}
    override suspend fun discoverNearbyNodes(): List<String> = emptyList()
}
