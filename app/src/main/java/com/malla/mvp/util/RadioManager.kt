package com.malla.mvp.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build

object RadioManager {
    private var originalBluetoothState = false
    private var originalWifiState = false
    private var initialized = false

    fun enableBluetooth(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !initialized) {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter
            if (adapter != null) {
                originalBluetoothState = adapter.isEnabled
                if (!originalBluetoothState) {
                    try {
                        adapter.enable()
                    } catch (e: SecurityException) {
                        // Permiso denegado
                    }
                }
            }
        }
    }

    fun enableWifi(context: Context) {
        if (!initialized) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                originalWifiState = wifiManager.isWifiEnabled
                if (!originalWifiState) {
                    try {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            wifiManager.isWifiEnabled = true
                        } else {
                            // En Android 10+ no se puede encender programáticamente,
                            // pero dejamos que el usuario lo haga manualmente.
                        }
                    } catch (e: SecurityException) { }
                }
            }
            initialized = true
        }
    }

    fun restoreStates(context: Context) {
        if (initialized) {
            // Restaurar Bluetooth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = btManager?.adapter
                if (adapter != null && adapter.isEnabled != originalBluetoothState) {
                    try {
                        if (originalBluetoothState) adapter.enable() else adapter.disable()
                    } catch (e: SecurityException) { }
                }
            }
            // Restaurar WiFi (solo API < 29)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null && wifiManager.isWifiEnabled != originalWifiState) {
                    try {
                        wifiManager.isWifiEnabled = originalWifiState
                    } catch (e: SecurityException) { }
                }
            }
            initialized = false
        }
    }
}
