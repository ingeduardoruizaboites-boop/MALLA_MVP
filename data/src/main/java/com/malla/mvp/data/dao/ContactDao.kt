package com.malla.mvp.data.dao

import androidx.room.*
import com.malla.mvp.data.entity.ContactEntity
import com.malla.mvp.data.entity.ContactStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts WHERE status = 'CONFIRMED' ORDER BY localAlias ASC")
    fun observeConfirmedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE status = 'PENDING' ORDER BY addedAt DESC")
    fun observePendingRequests(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE pubKeyBase64 = :pubKey LIMIT 1")
    suspend fun getContact(pubKey: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: ContactEntity)

    @Query("UPDATE contacts SET status = :status WHERE pubKeyBase64 = :pubKey")
    suspend fun updateStatus(pubKey: String, status: ContactStatus)

    @Query("DELETE FROM contacts WHERE pubKeyBase64 = :pubKey")
    suspend fun deleteContact(pubKey: String)

    @Query("SELECT COUNT(*) FROM contacts WHERE status = 'CONFIRMED'")
    suspend fun confirmedCount(): Int
}
