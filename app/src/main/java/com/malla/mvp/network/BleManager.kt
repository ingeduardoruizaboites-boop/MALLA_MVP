package com.malla.mvp.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

object BleManager {
    private const val TAG = "BleManager"
    private val serviceUuid = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb")
    private var adapter: BluetoothAdapter? = null
    private var scanner: BluetoothLeScanner? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    private val _foundDevices = MutableStateFlow<List<String>>(emptyList())
    val foundDevices: StateFlow<List<String>> = _foundDevices

    fun start(context: Context) {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = btManager.adapter
        if (adapter == null || !adapter!!.isEnabled) return
        scanner = adapter!!.bluetoothLeScanner
        advertiser = adapter!!.bluetoothLeAdvertiser
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        scanner?.startScan(null, scanSettings, scanCallback)
    }

    fun startAdvertising() {
        if (adapter == null || !adapter!!.isEnabled) {
            Log.w(TAG, "No se puede iniciar advertising: Bluetooth no disponible")
            return
        }
        if (advertiser == null) {
            advertiser = adapter!!.bluetoothLeAdvertiser
        }
        if (isAdvertising) {
            Log.d(TAG, "Advertising ya está activo")
            return
        }

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
        Log.d(TAG, "Advertising BLE iniciado con UUID: $serviceUuid")
    }

    fun stopAdvertising() {
        if (!isAdvertising) return
        advertiser?.stopAdvertising(advertiseCallback)
        isAdvertising = false
        Log.d(TAG, "Advertising BLE detenido")
    }

    fun stop() {
        scanner?.stopScan(scanCallback)
        stopAdvertising()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: device.address
            Log.d(TAG, "Dispositivo BLE encontrado: $name")
            _foundDevices.value = _foundDevices.value + name
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "Advertising iniciado con éxito")
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
            Log.e(TAG, "Fallo al iniciar advertising: $errorMsg")
        }
    }
}
