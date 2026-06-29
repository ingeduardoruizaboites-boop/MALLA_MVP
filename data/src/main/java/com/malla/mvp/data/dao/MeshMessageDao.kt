package com.malla.mvp.data.dao

import androidx.room.*
import com.malla.mvp.data.entity.MeshMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshMessageDao {
    @Query("SELECT * FROM mesh_messages ORDER BY timestamp ASC")
    fun getAll(): Flow<List<MeshMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MeshMessageEntity)

    @Query("DELETE FROM mesh_messages")
    suspend fun deleteAll()
}
