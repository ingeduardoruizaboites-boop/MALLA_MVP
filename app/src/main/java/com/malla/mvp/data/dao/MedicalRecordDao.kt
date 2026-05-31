package com.malla.mvp.data.dao

import androidx.room.*
import com.malla.mvp.data.entity.MedicalRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM medical_records")
    fun getAll(): Flow<List<MedicalRecordEntity>>
}
