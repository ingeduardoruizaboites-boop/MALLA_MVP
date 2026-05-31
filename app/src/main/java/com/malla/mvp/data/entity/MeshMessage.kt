package com.malla.mvp.data.entity

data class MeshMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOwn: Boolean = false,
    val status: Int = 0,  // 0=enviado, 1=entregado, 2=leído
    val type: Int = 0,    // 0=normal, 1=ACK entrega, 2=ACK lectura
    val originalMessageId: String? = null // para ACKs, el id del mensaje original
)
