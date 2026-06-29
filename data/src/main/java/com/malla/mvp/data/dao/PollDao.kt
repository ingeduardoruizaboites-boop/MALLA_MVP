package com.malla.mvp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.malla.mvp.data.entity.PollEntity
import com.malla.mvp.data.entity.PollOptionEntity
import com.malla.mvp.data.entity.PollVoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PollDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoll(poll: PollEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: PollOptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: PollVoteEntity)

    @Query("SELECT * FROM polls WHERE groupId = :groupId ORDER BY createdAt ASC")
    fun getPollsForGroup(groupId: String): Flow<List<PollEntity>>

    @Query("SELECT * FROM poll_options WHERE pollId = :pollId")
    fun getOptionsForPoll(pollId: String): Flow<List<PollOptionEntity>>

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId")
    suspend fun getVotesForPoll(pollId: String): List<PollVoteEntity>

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId AND userId = :userId")
    suspend fun getUserVote(pollId: String, userId: String): PollVoteEntity?

    @Query("DELETE FROM poll_votes WHERE pollId = :pollId AND userId = :userId")
    suspend fun deleteUserVote(pollId: String, userId: String)

    @Query("UPDATE poll_options SET voteCount = voteCount + :delta WHERE id = :optionId")
    suspend fun incrementVoteCount(optionId: String, delta: Int)
}
