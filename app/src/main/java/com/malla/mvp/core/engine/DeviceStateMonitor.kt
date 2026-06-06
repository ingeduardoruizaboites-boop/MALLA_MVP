package com.malla.mvp.core.engine

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.malla.mvp.network.BleManager
import com.malla.mvp.network.ConnectivityMonitor
import com.malla.mvp.core.engine.MeshSimulator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

object DeviceStateMonitor {
    private const val TAG = "DeviceStateMonitor"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null

    private val _state = MutableStateFlow(DeviceState())
    val state: StateFlow<DeviceState> = _state.asStateFlow()

    fun start(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "[DSM:LIFE] Iniciando DeviceStateMonitor")
        scope.launch {
            combine(
                batteryFlow(),
                connectivityFlow(),
                bleNodesFlow(),
                systemLoadFlow()
            ) { battery, connectivity, bleNodes, sysLoad ->
                val (ramMB, cpuTemp, deviceLoad) = sysLoad
                val hasInternet = connectivity.first
                val availableLevels = buildAvailableLevels(hasInternet, connectivity.second, connectivity.third)
                DeviceState(
                    batteryLevel = battery.first,
                    isCharging = battery.second,
                    currentLevel = if (hasInternet) MeshLevel.ONLINE_WIFI else MeshLevel.BLE,
                    availableLevels = availableLevels,
                    nearbyNodes = bleNodes,
                    hasInternetConnection = if (MeshSimulator.isSimulated.value) false else hasInternet,
                    deviceLoad = deviceLoad,
                    freeRamMB = ramMB,
                    cpuTemperature = cpuTemp,
                    isSmsAvailable = false, // Fase 2
                    isNfcAvailable = false, // Fase 2
                    lastUpdated = System.currentTimeMillis()
                )
            }.collect { newState ->
                _state.value = newState
                Log.d(TAG, "[DSM:STATE] Batería:${newState.batteryLevel}% RAM:${newState.freeRamMB}MB Nodos:${newState.nearbyNodes}")
            }
        }
    }

    fun stop() {
        Log.d(TAG, "[DSM:LIFE] Deteniendo DeviceStateMonitor")
        scope.coroutineContext.cancel()
    }

    private fun batteryFlow(): Flow<Pair<Int, Boolean>> = flow {
        while (currentCoroutineContext().isActive) {
            val ctx = appContext ?: break
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val pct = if (scale > 0) (level * 100 / scale) else 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                          status == BatteryManager.BATTERY_STATUS_FULL
            emit(Pair(pct, charging))
            delay(30_000L)
        }
    }

    private fun connectivityFlow(): Flow<Triple<Boolean, Boolean, Boolean>> = flow {
        ConnectivityMonitor.isOnline.collect { isOnline ->
            // Asumimos WiFi si está online (simplificación MVP)
            emit(Triple(isOnline, isOnline, false))
        }
    }

    private fun bleNodesFlow(): Flow<Int> = flow {
        BleManager.foundBluetoothDevices.collect { devices ->
            emit(devices.size)
        }
    }

    private fun systemLoadFlow(): Flow<Triple<Int, Double, DeviceLoad>> = flow {
        while (currentCoroutineContext().isActive) {
            val ctx = appContext ?: break
            val ramMB = getFreeRamMB(ctx)
            val cpuTemp = readCpuTemperature()
            val load = when {
                ramMB < 15 -> DeviceLoad.CRITICAL
                ramMB < 30 -> DeviceLoad.HEAVY
                ramMB < 60 -> DeviceLoad.MODERATE
                else -> DeviceLoad.IDLE
            }
            emit(Triple(ramMB, cpuTemp, load))
            delay(30_000L)
        }
    }

    private fun getFreeRamMB(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.availMem / (1024 * 1024)).toInt()
    }

    private fun readCpuTemperature(): Double {
        return try {
            val file = File("/sys/class/thermal/thermal_zone0/temp")
            if (file.exists()) {
                file.readText().trim().toDouble() / 1000.0
            } else 0.0
        } catch (_: Exception) { 0.0 }
    }

    private fun buildAvailableLevels(hasInternet: Boolean, hasWifi: Boolean, hasMobile: Boolean): List<MeshLevel> {
        val levels = mutableListOf<MeshLevel>()
        if (hasInternet && hasWifi) levels.add(MeshLevel.ONLINE_WIFI)
        if (hasInternet && hasMobile) levels.add(MeshLevel.ONLINE_MOBILE)
        if (hasWifi) levels.add(MeshLevel.WIFI_DIRECT)
        levels.add(MeshLevel.BLE)
        levels.add(MeshLevel.NO_SIGNAL)
        return levels
    }
}
