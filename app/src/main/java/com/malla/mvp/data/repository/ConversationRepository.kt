package com.malla.mvp.data.repository

import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.dao.ConversationDao
import com.malla.mvp.data.dao.MessageDao
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class ConversationRepository(private val db: AppDatabase) {

    private val conversationDao: ConversationDao = db.conversationDao()
    private val messageDao: MessageDao = db.messageDao()

    fun observeAll(): Flow<List<ConversationWithLastMessage>> {
        val conversationsFlow = conversationDao.getAllConversations()
        val messagesFlow = messageDao.observeAllMessages()

        return conversationsFlow.combine(messagesFlow) { conversations, messages ->
            conversations.map { conv ->
                val lastMsg = messages
                    .filter { it.conversationId == conv.id }
                    .maxByOrNull { it.timestamp }
                ConversationWithLastMessage(
                    conversation = conv,
                    lastMessage = lastMsg
                )
            }
        }
    }

    suspend fun insertInitialDemoDataIfNeeded() {
        if (conversationDao.getCount() > 0) return

        val convs = listOf(
            ConversationEntity(
                id = "conv_demo_1",
                title = "Elena",
                lastMessage = "¿Vamos a la plaza hoy?",
                timestamp = System.currentTimeMillis() - 300_000,
                unreadCount = 2
            ),
            ConversationEntity(
                id = "conv_demo_2",
                title = "Carlos",
                lastMessage = "Archivo enviado correctamente",
                timestamp = System.currentTimeMillis() - 900_000,
                unreadCount = 0
            ),
            ConversationEntity(
                id = "conv_demo_3",
                title = "Marta",
                lastMessage = "¡Feliz cumpleaños! 🎂",
                timestamp = System.currentTimeMillis() - 3_600_000,
                unreadCount = 1
            ),
            ConversationEntity(
                id = "conv_demo_4",
                title = "Andrés",
                lastMessage = "Nos vemos mañana",
                timestamp = System.currentTimeMillis() - 86_400_000,
                unreadCount = 0
            ),
            ConversationEntity(
                id = "conv_demo_5",
                title = "Sofía",
                lastMessage = "¿Has visto el nuevo nodo?",
                timestamp = System.currentTimeMillis() - 10_800_000,
                unreadCount = 3
            )
        )

        val messages = listOf(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_demo_1",
                content = "¿Vamos a la plaza hoy?",
                timestamp = System.currentTimeMillis() - 300_000,
                isOwn = false,
                status = 1
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_demo_2",
                content = "Archivo enviado correctamente",
                timestamp = System.currentTimeMillis() - 900_000,
                isOwn = true,
                status = 2
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_demo_3",
                content = "¡Feliz cumpleaños! 🎂",
                timestamp = System.currentTimeMillis() - 3_600_000,
                isOwn = false,
                status = 1
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_demo_4",
                content = "Nos vemos mañana",
                timestamp = System.currentTimeMillis() - 86_400_000,
                isOwn = true,
                status = 2
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_demo_5",
                content = "¿Has visto el nuevo nodo?",
                timestamp = System.currentTimeMillis() - 10_800_000,
                isOwn = false,
                status = 1
            )
        )

        conversationDao.insertAll(convs)
        messageDao.insertAll(messages)
    }
}

data class ConversationWithLastMessage(
    val conversation: ConversationEntity,
    val lastMessage: MessageEntity?
)
