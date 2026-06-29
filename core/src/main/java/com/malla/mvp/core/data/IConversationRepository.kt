package com.malla.mvp.core.data

import kotlinx.coroutines.flow.Flow

data class ConversationData(
    val id: String,
    val name: String,
    val lastMessage: String?,
    val timestamp: Long
)

interface IConversationRepository {
    fun observeConversations(): Flow<List<ConversationData>>
    suspend fun upsertConversation(conversation: ConversationData)
    suspend fun getConversation(id: String): ConversationData?
}
