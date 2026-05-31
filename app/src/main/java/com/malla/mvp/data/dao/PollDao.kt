package com.malla.mvp.data.dao

import androidx.room.*
import com.malla.mvp.data.entity.PollEntity
import com.malla.mvp.data.entity.PollOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PollDao {
    @Query("SELECT * FROM polls WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getPollsForGroup(groupId: String): Flow<List<PollEntity>>

    @Query("SELECT * FROM poll_options WHERE pollId = :pollId")
    fun getOptionsForPoll(pollId: String): Flow<List<PollOptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoll(poll: PollEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: PollOptionEntity)

    @Query("UPDATE poll_options SET voteCount = voteCount + 1 WHERE id = :optionId")
    suspend fun incrementVote(optionId: String)
}
