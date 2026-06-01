package com.malla.mvp.network

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Orquestador de la conexión mesh automática.
 * 
 * Flujo:
 * 1. Observa los dispositivos BLE encontrados por BleManager.
 * 2. Para cada dispositivo nuevo, obtiene su IP via GATT.
 * 3. Si obtiene la IP, inicia la conexión TCP via NetworkService.
 * 4. El handshake ECDH ocurre automáticamente en NetworkService.
 *
 * Tags de log: [MC:SCAN], [MC:GATT], [MC:TCP], [MC:ERR], [MC:LIFE]
 */
object MeshConnector {
    private const val TAG = "MeshConnector"

    // Códigos de error (para diagnóstico)
    // MC-E001: Fallo al leer IP via GATT
    // MC-E002: IP obtenida pero nula o vacía
    // MC-E003: Fallo al conectar TCP
    // MC-E004: Dispositivo ya conectado

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observationJob: Job? = null

    // Caché local de direcciones IP ya conectadas para no reintentar
    private val connectedIps = mutableSetOf<String>()

    /**
     * Inicia el proceso de conexión automática.
     * Debe llamarse cuando el escaneo BLE está activo.
     */
    fun start() {
        Log.d(TAG, "[MC:LIFE] Iniciando MeshConnector")
        // Cancelar observación anterior si existe
        observationJob?.cancel()

        observationJob = scope.launch {
            // Observar dispositivos BLE encontrados
            BleManager.foundBluetoothDevices.collect { devices ->
                Log.d(TAG, "[MC:SCAN] Dispositivos encontrados: ${devices.size}")
                for (device in devices) {
                    // Procesar cada dispositivo
                    processDevice(device)
                }
            }
        }
    }

    /**
     * Detiene el conector.
     */
    fun stop() {
        Log.d(TAG, "[MC:LIFE] Deteniendo MeshConnector")
        observationJob?.cancel()
        observationJob = null
    }

    /**
     * Procesa un dispositivo encontrado:
     * - Obtiene su IP por GATT
     * - Si es nueva IP, inicia conexión TCP
     */
    private suspend fun processDevice(device: BluetoothDevice) {
        val address = device.address
        Log.d(TAG, "[MC:SCAN] Procesando dispositivo: ${device.name ?: address} ($address)")

        try {
            // 1. Leer IP via GATT
            Log.d(TAG, "[MC:GATT] Solicitando IP a $address...")
            val ip = BleManager.connectAndReadIp(device)

            if (ip == null) {
                Log.w(TAG, "[MC:ERR] MC-E001: No se pudo obtener IP de $address")
                return
            }
            if (ip.isBlank() || ip == "Desconocida") {
                Log.w(TAG, "[MC:ERR] MC-E002: IP inválida obtenida de $address: $ip")
                return
            }

            Log.d(TAG, "[MC:GATT] IP obtenida de $address: $ip")

            // 2. Verificar si ya está conectado
            if (connectedIps.contains(ip)) {
                Log.d(TAG, "[MC:TCP] MC-E004: Ya conectado a $ip, ignorando")
                return
            }

            // 3. Conectar via TCP
            Log.d(TAG, "[MC:TCP] Iniciando conexión TCP a $ip:${NetworkService.DEFAULT_PORT}")
            NetworkService.connectToPeer(ip)

            // Registrar IP como conectada
            connectedIps.add(ip)
            Log.d(TAG, "[MC:TCP] Conexión iniciada a $ip")

        } catch (e: Exception) {
            Log.e(TAG, "[MC:ERR] MC-E003: Error procesando dispositivo $address: ${e.message}", e)
        }
    }
}
