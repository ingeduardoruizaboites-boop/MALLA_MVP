package com.malla.mvp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ContactStatus { PENDING, CONFIRMED, BLOCKED }

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val pubKeyBase64: String,
    val localAlias: String?,
    val addedVia: String, // "QR", "CODE", "NFC", "AGENDA"
    val status: ContactStatus = ContactStatus.PENDING,
    val addedAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long? = null
)
