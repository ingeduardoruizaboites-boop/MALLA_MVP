package com.malla.mvp.core.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface IBleManager {
    val foundBluetoothDevices: StateFlow<List<BluetoothDevice>>
    fun start(context: Context)
    fun stop()
    suspend fun connectAndReadIp(device: BluetoothDevice): String?
}
