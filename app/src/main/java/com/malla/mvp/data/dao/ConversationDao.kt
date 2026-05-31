package com.malla.mvp.data.dao

import androidx.room.*
import com.malla.mvp.data.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE isHidden = 0 ORDER BY timestamp DESC")
    fun getAllVisibleConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET lastMessage = :message, timestamp = :timestamp WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, message: String, timestamp: Long)

    @Query("UPDATE conversations SET isHidden = 1 WHERE id = :conversationId")
    suspend fun hideConversation(conversationId: String)

    @Query("UPDATE conversations SET chatBackgroundColor = :color WHERE id = :conversationId")
    suspend fun updateChatBackgroundColor(conversationId: String, color: Int)
}
