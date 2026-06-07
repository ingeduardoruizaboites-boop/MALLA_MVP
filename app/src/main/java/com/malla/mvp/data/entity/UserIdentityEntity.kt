package com.malla.mvp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_identity")
data class UserIdentityEntity(
    @PrimaryKey
    val pubKeyBase64: String,
    val pubKeyTruncated: String,
    val displayName: String,
    val avatarBase64: String?,
    val identiconSvg: String,
    val identitySealNonce: Int,
    val identitySealTimestamp: Long,
    val identitySealHash: String,
    val createdAt: Long,
    val isActive: Boolean = true
)
