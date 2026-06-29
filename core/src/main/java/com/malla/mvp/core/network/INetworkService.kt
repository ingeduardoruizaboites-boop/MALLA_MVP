package com.malla.mvp.core.network

import kotlinx.coroutines.flow.Flow

data class MeshMessage(
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: Int = 0,
    val originalMessageId: String? = null
)

interface INetworkService {
    val connectionState: Flow<Boolean>
    suspend fun sendMeshMessage(message: MeshMessage): Result<Unit>
    fun addMessageListener(listener: (MeshMessage) -> Unit)
    fun removeMessageListener(listener: (MeshMessage) -> Unit)
}
