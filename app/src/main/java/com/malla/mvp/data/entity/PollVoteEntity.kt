package com.malla.mvp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "poll_votes")
data class PollVoteEntity(
    @PrimaryKey val id: String,
    val pollId: String,
    val optionId: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)
