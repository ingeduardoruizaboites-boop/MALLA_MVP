package com.malla.mvp.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

object BleManager {
    private const val TAG = "BleManager"
    private val serviceUuid = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb")
    private val ipCharacteristicUuid = UUID.fromString("0000abcd-0001-1000-8000-00805f9b34fb")
    private var adapter: BluetoothAdapter? = null
    private var scanner: BluetoothLeScanner? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    fun getAdapter(): BluetoothAdapter? = adapter
    private var isAdvertising = false
    private var isScanningActive = false
    private var appContext: Context? = null

    private val _foundDevices = MutableStateFlow<List<String>>(emptyList())
    val foundDevices: StateFlow<List<String>> = _foundDevices
    private val _foundBluetoothDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundBluetoothDevices: StateFlow<List<BluetoothDevice>> = _foundBluetoothDevices

    fun start(context: Context) {
        appContext = context.applicationContext
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = btManager.adapter
        if (adapter == null || !adapter!!.isEnabled) return
        scanner = adapter!!.bluetoothLeScanner
        advertiser = adapter!!.bluetoothLeAdvertiser
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        scanner?.startScan(null, scanSettings, scanCallback)
        isScanningActive = true
    }

    fun startAdvertising() {
        if (adapter == null || !adapter!!.isEnabled) {
            Log.w(TAG, "[BLE:ADV] No se puede iniciar advertising: Bluetooth no disponible")
            return
        }
        if (advertiser == null) {
            advertiser = adapter!!.bluetoothLeAdvertiser
        }
        if (isAdvertising) {
            Log.d(TAG, "[BLE:ADV] Advertising ya está activo")
            return
        }

        // Verificar permiso BLUETOOTH_ADVERTISE en runtime (Android 12+)
        val context = appContext
        if (context != null && ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "[PERM:ERR] BLUETOOTH_ADVERTISE no concedido. No se puede iniciar advertising.")
            return
        }

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid(serviceUuid))
                .build()

            advertiser?.startAdvertising(settings, data, advertiseCallback)
            isAdvertising = true
            Log.d(TAG, "[BLE:ADV] Advertising BLE iniciado con UUID: $serviceUuid")
        } catch (e: SecurityException) {
            Log.e(TAG, "[PERM:ERR] SecurityException al iniciar advertising: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "[BLE:ADV:ERR] Error al iniciar advertising: ${e.message}", e)
        }
    }

    fun stopAdvertising() {
        if (!isAdvertising) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            Log.d(TAG, "[BLE:ADV] Advertising BLE detenido")
        } catch (e: Exception) {
            Log.e(TAG, "[BLE:ADV:ERR] Error al detener advertising: ${e.message}", e)
        }
    }

    fun stop() {
        scanner?.stopScan(scanCallback)
        isScanningActive = false
        stopAdvertising()
    }

    suspend fun connectAndReadIp(device: BluetoothDevice): String? =
        suspendCancellableCoroutine { continuation ->
            val context = appContext ?: run {
                Log.e(TAG, "[BLE:GATT] Contexto no inicializado")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val wasScanning = isScanningActive
            if (wasScanning) {
                scanner?.stopScan(scanCallback)
                isScanningActive = false
            }

            var gatt: BluetoothGatt? = null
            var ipResult: String? = null

            val callback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt?.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (ipResult == null && !continuation.isCompleted) {
                            Log.w(TAG, "[BLE:GATT] Desconectado sin obtener IP de ${device.address}")
                            continuation.resume(null)
                        }
                        gatt?.close()
                        if (wasScanning && appContext != null) {
                            start(appContext!!)
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service: BluetoothGattService? = gatt?.getService(serviceUuid)
                        val characteristic: BluetoothGattCharacteristic? = service?.getCharacteristic(ipCharacteristicUuid)
                        if (characteristic != null) {
                            gatt?.readCharacteristic(characteristic)
                        } else {
                            Log.w(TAG, "[BLE:GATT] Característica de IP no encontrada en ${gatt?.device?.address}")
                            gatt?.disconnect()
                        }
                    } else {
                        Log.w(TAG, "[BLE:GATT] Descubrimiento de servicios fallido: $status")
                        gatt?.disconnect()
                    }
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS && characteristic?.uuid == ipCharacteristicUuid) {
                        val bytes = characteristic?.value
                        ipResult = bytes?.toString(Charsets.UTF_8)
                        Log.d(TAG, "[BLE:GATT] IP leída de ${device.address}: $ipResult")
                    } else {
                        Log.w(TAG, "[BLE:GATT] Fallo al leer característica de IP: status=$status")
                    }
                    gatt?.disconnect()
                    if (ipResult != null && !continuation.isCompleted) {
                        continuation.resume(ipResult)
                    } else if (!continuation.isCompleted) {
                        continuation.resume(null)
                    }
                }
            }

            gatt = device.connectGatt(context, false, callback)
            if (gatt == null) {
                Log.e(TAG, "[BLE:GATT] No se pudo conectar a ${device.address}")
                if (wasScanning && appContext != null) start(appContext!!)
                continuation.resume(null)
            }
        }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: device.address
            Log.d(TAG, "[BLE:SCAN] Dispositivo BLE encontrado: $name")
            _foundDevices.value = _foundDevices.value + name
            if (!_foundBluetoothDevices.value.contains(device)) {
                _foundBluetoothDevices.value = _foundBluetoothDevices.value + device
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "[BLE:ADV] Advertising iniciado con éxito")
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            val errorMsg = when (errorCode) {
                AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Datos demasiado grandes"
                AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Demasiados advertisers activos"
                AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Ya estaba iniciado"
                AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Error interno"
                AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Característica no soportada"
                else -> "Error desconocido: $errorCode"
            }
            Log.e(TAG, "[BLE:ADV:ERR] Fallo al iniciar advertising: $errorMsg")
        }
    }
}
