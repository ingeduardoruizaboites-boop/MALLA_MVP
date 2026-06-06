package com.malla.mvp.core.transport

import android.content.Context
import com.malla.mvp.core.engine.MeshLevel
import com.malla.mvp.network.BleManager

class BleTransport(private val context: Context) : MeshTransport() {
    override val level = MeshLevel.BLE
    override val isAvailable: Boolean
        get() {
            val adapter = BleManager.getAdapter()
            return adapter != null && adapter.isEnabled
        }
    override val estimatedBatteryDrainPerMinute = 2

    override suspend fun initialize(): Boolean {
        BleManager.start(context)
        BleManager.startAdvertising()
        return true
    }

    override suspend fun dispose() {
        BleManager.stop()
    }

    override suspend fun discoverNearbyNodes(): List<String> {
        return BleManager.foundDevices.value
    }
}
