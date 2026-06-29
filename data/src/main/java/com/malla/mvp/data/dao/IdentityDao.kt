package com.malla.mvp.data.dao

import androidx.room.*
import com.malla.mvp.data.entity.UserIdentityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {

    @Query("SELECT * FROM user_identity WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveIdentity(): UserIdentityEntity?

    @Query("SELECT * FROM user_identity WHERE isActive = 1 LIMIT 1")
    fun observeActiveIdentity(): Flow<UserIdentityEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: UserIdentityEntity)

    @Query("UPDATE user_identity SET isActive = 0")
    suspend fun deactivateAll()

    @Query("SELECT COUNT(*) FROM user_identity WHERE isActive = 1")
    suspend fun hasActiveIdentity(): Int

    @Query("UPDATE user_identity SET displayName = :name WHERE isActive = 1")
    suspend fun updateDisplayName(name: String)
}
