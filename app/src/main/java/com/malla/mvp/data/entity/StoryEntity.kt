package com.malla.mvp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val imageUri: String = "",   // Uri simulada
    val timestamp: Long = System.currentTimeMillis(),
    val seen: Boolean = false
)
