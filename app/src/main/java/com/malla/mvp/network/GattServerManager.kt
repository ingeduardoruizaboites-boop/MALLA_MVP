package com.malla.mvp.network

import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.UUID

object GattServerManager {
    private const val TAG = "GattServerManager"

    val SERVICE_UUID: UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb")
    private val IP_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000abcd-0001-1000-8000-00805f9b34fb")

    private var gattServer: BluetoothGattServer? = null
    private var isRunning = false

    fun start(context: Context): Boolean {
        // Verificar permiso BLUETOOTH_CONNECT en runtime (Android 12+)
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "[PERM:ERR] BLUETOOTH_CONNECT no concedido. No se puede iniciar GATT.")
            return false
        }

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "[GATT:ERR] Bluetooth no disponible para servidor GATT")
            return false
        }

        return try {
            gattServer = btManager.openGattServer(context, gattServerCallback).apply {
                val service = BluetoothGattService(
                    SERVICE_UUID,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY
                )
                val ipCharacteristic = BluetoothGattCharacteristic(
                    IP_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ
                )
                service.addCharacteristic(ipCharacteristic)
                addService(service)
            }
            isRunning = true
            Log.d(TAG, "[GATT] Servidor GATT iniciado con UUID: $SERVICE_UUID")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "[PERM:ERR] SecurityException al abrir GATT: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "[GATT:ERR] Error al iniciar servidor: ${e.message}", e)
            false
        }
    }

    fun stop() {
        try {
            gattServer?.close()
            gattServer = null
            isRunning = false
            Log.d(TAG, "[GATT] Servidor GATT detenido")
        } catch (e: Exception) {
            Log.e(TAG, "[GATT:ERR] Error al detener servidor: ${e.message}", e)
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            try {
                if (characteristic.uuid == IP_CHARACTERISTIC_UUID) {
                    val localIp = NetworkService.getLocalIpAddress()
                    Log.d(TAG, "[GATT] Sirviendo IP local a ${device.address}: $localIp")
                    val response = localIp.toByteArray(Charsets.UTF_8)
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        response
                    )
                } else {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED,
                        0,
                        null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "[GATT:ERR] Error en onCharacteristicReadRequest: ${e.message}", e)
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "[GATT] Dispositivo ${device.address} desconectado del GATT")
            }
        }
    }
}
