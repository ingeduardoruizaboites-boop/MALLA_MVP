package com.malla.mvp.core.engine

import java.time.Instant

data class DeviceState(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val currentLevel: MeshLevel = MeshLevel.NO_SIGNAL,
    val availableLevels: List<MeshLevel> = emptyList(),
    val pendingMessages: Int = 0,
    val nearbyNodes: Int = 0,
    val highestPriority: MessagePriority = MessagePriority.NORMAL,
    val deviceLoad: DeviceLoad = DeviceLoad.IDLE,
    val hasInternetConnection: Boolean = false,
    val isSmsAvailable: Boolean = false,
    val isNfcAvailable: Boolean = false,
    val cpuTemperature: Double = 0.0,
    val freeRamMB: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
