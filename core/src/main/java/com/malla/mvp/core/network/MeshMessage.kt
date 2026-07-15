package com.malla.mvp.core.network

data class MeshMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: Int = 0,
    val status: Int = 0,
    val originalMessageId: String? = null,
    val recipientId: String? = null
)
