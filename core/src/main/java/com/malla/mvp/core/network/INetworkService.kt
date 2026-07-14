package com.malla.mvp.core.network

import kotlinx.coroutines.flow.Flow


interface INetworkService {
    val connectionState: Flow<Boolean>
    suspend fun sendMeshMessage(message: MeshMessage): Result<Unit>
    fun addMessageListener(listener: (MeshMessage) -> Unit)
    fun removeMessageListener(listener: (MeshMessage) -> Unit)
}
