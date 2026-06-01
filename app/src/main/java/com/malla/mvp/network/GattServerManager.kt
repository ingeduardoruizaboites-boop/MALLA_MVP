package com.malla.mvp.network

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID

/**
 * Servidor GATT que expone la dirección IP local del dispositivo
 * para que otros nodos MALLA puedan obtenerla y establecer una conexión TCP.
 */
object GattServerManager {
    private const val TAG = "GattServerManager"

    // UUIDs del servicio y característica (deben coincidir con los del cliente en BleManager)
    val SERVICE_UUID: UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb")
    private val IP_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000abcd-0001-1000-8000-00805f9b34fb")

    private var gattServer: BluetoothGattServer? = null
    private var isRunning = false

    /**
     * Inicia el servidor GATT. Debe llamarse después de que Bluetooth esté habilitado.
     */
    fun start(context: Context): Boolean {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth no disponible para servidor GATT")
            return false
        }

        gattServer = btManager.openGattServer(context, gattServerCallback).apply {
            // Crear servicio y característica
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
        Log.d(TAG, "Servidor GATT iniciado con UUID: $SERVICE_UUID")
        return true
    }

    /**
     * Detiene el servidor GATT.
     */
    fun stop() {
        gattServer?.close()
        gattServer = null
        isRunning = false
        Log.d(TAG, "Servidor GATT detenido")
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == IP_CHARACTERISTIC_UUID) {
                val localIp = NetworkService.getLocalIpAddress()
                Log.d(TAG, "Sirviendo IP local a ${device.address}: $localIp")
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
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Dispositivo ${device.address} desconectado del GATT")
            }
        }
    }
}
