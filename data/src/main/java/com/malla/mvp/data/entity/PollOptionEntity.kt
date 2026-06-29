package com.malla.mvp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "poll_options")
data class PollOptionEntity(
    @PrimaryKey val id: String,
    val pollId: String,
    val text: String,
    val voteCount: Int = 0
)
