package com.malla.mvp.core.transport

import com.malla.mvp.core.engine.MeshLevel

abstract class MeshTransport {
    abstract val level: MeshLevel
    abstract val isAvailable: Boolean
    abstract val estimatedBatteryDrainPerMinute: Int
    abstract suspend fun initialize(): Boolean
    abstract suspend fun dispose()
    abstract suspend fun discoverNearbyNodes(): List<String>
}
