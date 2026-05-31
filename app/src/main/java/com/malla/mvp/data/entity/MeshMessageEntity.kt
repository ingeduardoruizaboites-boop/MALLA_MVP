package com.malla.mvp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesh_messages")
data class MeshMessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOwn: Boolean = false,
    val status: Int = 0  // 0=enviado, 1=entregado, 2=leído
)
