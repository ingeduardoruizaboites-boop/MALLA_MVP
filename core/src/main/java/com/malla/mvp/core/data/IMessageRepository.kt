package com.malla.mvp.core.data

import kotlinx.coroutines.flow.Flow

data class MessageData(
    val id: String,
    val conversationId: String,
    val content: String,
    val timestamp: Long,
    val isOwn: Boolean,
    val mediaUri: String? = null,
    val expireAt: Long? = null,
    val viewOnce: Boolean = false,
    val transport: String? = null
)

interface IMessageRepository {
    fun observeMessages(conversationId: String): Flow<List<MessageData>>
    suspend fun saveMessage(message: MessageData)
    suspend fun getLastMessage(conversationId: String): MessageData?
    suspend fun updateMessageStatus(messageId: String, status: Int)
    suspend fun markConversationAsRead(conversationId: String)
    suspend fun getUnreadMessages(conversationId: String): List<MessageData>
    suspend fun getPendingMessages(): List<MessageData>
}
