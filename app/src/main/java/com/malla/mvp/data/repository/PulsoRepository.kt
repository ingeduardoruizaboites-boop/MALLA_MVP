package com.malla.mvp.data.repository

import com.malla.mvp.network.BleManager
import com.malla.mvp.network.ConnectivityMonitor
import com.malla.mvp.network.NetworkService
import com.malla.mvp.ui.screen.ConnectionType
import com.malla.mvp.ui.screen.MeshNode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object PulsoRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    val isOnline: StateFlow<Boolean> = ConnectivityMonitor.isOnline

    val meshNodes: StateFlow<List<MeshNode>> =
        BleManager.foundBluetoothDevices.combine(NetworkService.connectedClientsCount) { devices, connectedCount ->
            devices.mapIndexed { index, device ->
                MeshNode(
                    id = device.address,
                    alias = device.name?.take(2)?.uppercase() ?: "?",
                    relativeX = (0.15f + (index % 4) * 0.2f).coerceIn(0.1f, 0.9f),
                    relativeY = (0.2f + (index / 3) * 0.25f).coerceIn(0.1f, 0.9f),
                    hopCount = 1,
                    connectionType = ConnectionType.BLE,
                    signalStrength = -50 + (index % 4) * 10,
                    latencyMs = 2 + (index % 2) * 5,
                    bluetoothDevice = device
                )
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    val connectedPeersCount: StateFlow<Int> = NetworkService.connectedClientsCount
    val relayedMessagesCount = MutableStateFlow(0)
}
