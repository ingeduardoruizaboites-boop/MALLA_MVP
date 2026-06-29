package com.malla.mvp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val lastMessageStatus: Int = 0,
    val isHidden: Boolean = false,
    val isGroup: Boolean = false,
    val chatBackgroundColor: Int? = null   // Color en ARGB como Int (ej. 0xFF1A1A1A.toInt())
)
