package com.malla.mvp.core.data

import kotlinx.coroutines.flow.Flow

data class PollData(val id: String, val groupId: String, val question: String, val creatorId: String)
data class PollOptionData(val id: String, val pollId: String, val text: String, val voteCount: Int = 0)

interface IPollRepository {
    fun observePollsForGroup(groupId: String): Flow<List<PollData>>
    fun observeOptionsForPoll(pollId: String): Flow<List<PollOptionData>>
    suspend fun createPoll(poll: PollData, options: List<PollOptionData>)
    suspend fun incrementVoteCount(optionId: String, amount: Int)
}
