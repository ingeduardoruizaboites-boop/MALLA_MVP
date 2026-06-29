package com.malla.mvp.core.engine

import android.util.Log

object DecisionEngine {
    private const val TAG = "DecisionEngine"

    fun decide(state: DeviceState): CommunicationDecision {
        // Verificar modo emergencia forzado por usuario (Regla 0 ampliada)
        if (EmergencyMode.isActive.value) {
            val best = bestAvailable(state)
            return CommunicationDecision(
                primaryTransport = best,
                secondaryTransport = secondBestAvailable(state, best),
                scanIntervalSeconds = 5,
                maxPayloadBytes = 512,
                ttlHops = 24,
                useCompression = true,
                activateBloomFilter = true,
                reasoning = "Modo emergencia forzado por usuario - todos los radios activos"
            )
        }

        // Regla 0: Emergencia por prioridad de mensaje
        if (state.highestPriority == MessagePriority.EMERGENCY) {
            val best = bestAvailable(state)
            val second = secondBestAvailable(state, best)
            return CommunicationDecision(
                primaryTransport = best,
                secondaryTransport = second,
                scanIntervalSeconds = 5,
                maxPayloadBytes = 512,
                ttlHops = 24,
                useCompression = true,
                activateBloomFilter = true,
                reasoning = "Regla 0: Emergencia - todos los radios activos"
            )
        }

        // Regla 9: Sin señal radio
        val radioLevels = listOf(MeshLevel.NFC, MeshLevel.QR_CODE, MeshLevel.FLASH_LIGHT, MeshLevel.NO_SIGNAL)
        if (state.availableLevels.all { it in radioLevels }) {
            return CommunicationDecision(
                primaryTransport = if (state.isNfcAvailable) MeshLevel.NFC else MeshLevel.QR_CODE,
                secondaryTransport = MeshLevel.QR_CODE,
                scanIntervalSeconds = 300,
                maxPayloadBytes = 64,
                ttlHops = 1,
                reasoning = "Regla 9: Sin radios - modo contacto físico"
            )
        }

        // Regla 8: SMS Bridge
        val hasSms = state.isSmsAvailable && state.pendingMessages > 0 &&
                state.highestPriority >= MessagePriority.HIGH && !state.hasInternetConnection

        // Regla 1: Online WiFi
        if (state.hasInternetConnection && state.availableLevels.contains(MeshLevel.ONLINE_WIFI)) {
            return CommunicationDecision(
                primaryTransport = MeshLevel.ONLINE_WIFI,
                secondaryTransport = if (hasSms) MeshLevel.SMS_BRIDGE else null,
                scanIntervalSeconds = 0,
                maxPayloadBytes = Int.MAX_VALUE,
                ttlHops = 0,
                reasoning = "Regla 1: Online con WiFi"
            )
        }

        // Regla 2: Online con Datos
        if (state.hasInternetConnection && state.availableLevels.contains(MeshLevel.ONLINE_MOBILE)) {
            return CommunicationDecision(
                primaryTransport = MeshLevel.ONLINE_MOBILE,
                secondaryTransport = if (hasSms) MeshLevel.SMS_BRIDGE else null,
                scanIntervalSeconds = 0,
                maxPayloadBytes = Int.MAX_VALUE,
                ttlHops = 0,
                useCompression = true,
                reasoning = "Regla 2: Online con Datos móviles (compresión activada)"
            )
        }

        // Reglas 3-6: Modo Mesh (sin internet)
        val battery = state.batteryLevel

        // Regla 6: Supervivencia (<10%)
        if (battery < 10 && !state.isCharging) {
            return CommunicationDecision(
                primaryTransport = MeshLevel.BLE,
                scanIntervalSeconds = 300,
                maxPayloadBytes = 64,
                ttlHops = 3,
                reasoning = "Regla 6: Modo supervivencia (batería <10%)"
            )
        }

        // Regla 5: Batería baja (10-20%)
        if (battery in 10..20) {
            return CommunicationDecision(
                primaryTransport = MeshLevel.BLE,
                scanIntervalSeconds = 120,
                maxPayloadBytes = 256,
                ttlHops = 6,
                useCompression = true,
                reasoning = "Regla 5: Batería baja (10-20%) - solo BLE, compresión obligatoria"
            )
        }

        // Regla 4: Batería media (20-50%)
        if (battery in 20..50) {
            return CommunicationDecision(
                primaryTransport = MeshLevel.BLE,
                secondaryTransport = MeshLevel.WIFI_DIRECT,
                scanIntervalSeconds = 45,
                maxPayloadBytes = 1024,
                ttlHops = 12,
                reasoning = "Regla 4: Batería media (20-50%) - BLE primario, WiFi Direct secundario"
            )
        }

        // Regla 3: Batería alta (>50%)
        if (battery > 50) {
            return CommunicationDecision(
                primaryTransport = MeshLevel.WIFI_DIRECT,
                secondaryTransport = MeshLevel.BLE,
                scanIntervalSeconds = 15,
                maxPayloadBytes = 4096,
                ttlHops = 24,
                reasoning = "Regla 3: Batería alta (>50%) - WiFi Direct primario"
            )
        }

        // Regla 7: Carga pesada de CPU/RAM
        if (state.deviceLoad == DeviceLoad.HEAVY || state.deviceLoad == DeviceLoad.CRITICAL) {
            return CommunicationDecision(
                primaryTransport = MeshLevel.BLE,
                scanIntervalSeconds = 120,
                maxPayloadBytes = 128,
                ttlHops = 3,
                useCompression = true,
                reasoning = "Regla 7: Carga pesada de CPU/RAM - BLE mínimo"
            )
        }

        // Fallback seguro
        Log.w(TAG, "Ninguna regla específica aplicó. Usando fallback seguro.")
        return CommunicationDecision.defaultSafe()
    }

    private fun bestAvailable(state: DeviceState): MeshLevel {
        val preferred = listOf(
            MeshLevel.ONLINE_WIFI, MeshLevel.ONLINE_MOBILE, MeshLevel.WIFI_DIRECT,
            MeshLevel.BLE, MeshLevel.BLUETOOTH_CLASSIC, MeshLevel.NFC,
            MeshLevel.ULTRASOUND, MeshLevel.SMS_BRIDGE, MeshLevel.QR_CODE
        )
        return preferred.firstOrNull { state.availableLevels.contains(it) } ?: MeshLevel.NO_SIGNAL
    }

    private fun secondBestAvailable(state: DeviceState, exclude: MeshLevel): MeshLevel? {
        val preferred = listOf(
            MeshLevel.ONLINE_WIFI, MeshLevel.ONLINE_MOBILE, MeshLevel.WIFI_DIRECT,
            MeshLevel.BLE, MeshLevel.BLUETOOTH_CLASSIC, MeshLevel.NFC,
            MeshLevel.ULTRASOUND, MeshLevel.SMS_BRIDGE, MeshLevel.QR_CODE
        )
        return preferred.firstOrNull { it != exclude && state.availableLevels.contains(it) }
    }
}
