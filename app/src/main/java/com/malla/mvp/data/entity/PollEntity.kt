package com.malla.mvp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "polls")
data class PollEntity(
    @PrimaryKey val id: String,
    val createdAt: Long = System.currentTimeMillis(),
    val groupId: String,
    val question: String,
    val creatorId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false
)
