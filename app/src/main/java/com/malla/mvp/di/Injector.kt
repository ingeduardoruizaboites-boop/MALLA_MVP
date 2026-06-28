package com.malla.mvp.di

import android.content.Context
import com.malla.mvp.App
import com.malla.mvp.core.data.*
import com.malla.mvp.core.engine.DeviceProfile
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.core.engine.PremiumManager
import com.malla.mvp.core.network.INetworkService
import com.malla.mvp.core.network.MeshMessage as CoreMeshMessage
import com.malla.mvp.core.notification.INotificationHelper
import com.malla.mvp.core.transport.FlashlightTransport
import com.malla.mvp.core.util.ILogger
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object Injector {
    lateinit var messageBridge: MessageBridge
    lateinit var flashlightTransport: FlashlightTransport
    lateinit var messageRepo: IMessageRepository
    lateinit var networkService: INetworkService

    fun init(context: Context) {
        val db = AppDatabase.getInstance(context)

        // Inicializar transporte Faro (luz/cámara)
        flashlightTransport = FlashlightTransport(context)
        flashlightTransport.start()

        // Adaptador de red (NetworkService -> INetworkService)
        val networkService = object : INetworkService {
            override val connectionState: Flow<Boolean> = NetworkService.connectedClientsCount.map { it > 0 }
            override suspend fun sendMeshMessage(message: CoreMeshMessage): Result<Unit> {
                return try {
                    val nsMessage = com.malla.mvp.network.MeshMessage(
                        content = message.content,
                        senderId = message.senderId,
                        timestamp = message.timestamp,
                        type = message.type,
                        originalMessageId = message.originalMessageId
                    )
                    NetworkService.sendMessage(nsMessage)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            override fun addMessageListener(listener: (CoreMeshMessage) -> Unit) {
                CoroutineScope(Dispatchers.IO).launch {
                    NetworkService.messages.collect { nsMsg ->
                        listener(CoreMeshMessage(nsMsg.senderId, nsMsg.content, nsMsg.timestamp, nsMsg.type, nsMsg.originalMessageId))
                    }
                }
            }
            override fun removeMessageListener(listener: (CoreMeshMessage) -> Unit) {}
        }
        this.networkService = networkService

        // Repositorio de mensajes (Room)
        val messageRepo = object : IMessageRepository {
            override fun observeMessages(conversationId: String) = db!!.messageDao().observeAllMessages().map { entities -> entities.filter { it.conversationId == conversationId }.map { MessageData(it.id, it.conversationId, it.content, it.timestamp, it.isOwn) } }
            override suspend fun saveMessage(message: MessageData) { db.messageDao().insertMessage(com.malla.mvp.data.entity.MessageEntity(message.id, message.conversationId, message.content, message.timestamp, message.isOwn, 1, mediaUri = message.mediaUri, expireAt = message.expireAt, viewOnce = message.viewOnce, transport = message.transport)) }
            override suspend fun getLastMessage(conversationId: String): MessageData? = null
            override suspend fun updateMessageStatus(messageId: String, status: Int) {}
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun getUnreadMessages(conversationId: String): List<MessageData> = emptyList()
            override suspend fun getPendingMessages(): List<MessageData> = emptyList()
        }
        this.messageRepo = messageRepo

        // Repositorio de conversaciones
        val conversationRepo = object : IConversationRepository {
            override fun observeConversations() = flowOf(emptyList<ConversationData>())
            override suspend fun upsertConversation(conversation: ConversationData) {}
            override suspend fun getConversation(id: String): ConversationData? = null
        }

        // Logger y notificaciones
        val logger = object : ILogger { override fun log(tag: String, message: String) { LogBuffer.add(tag, message) } }
        val notificationHelper = object : INotificationHelper { override fun showNotification(title: String, message: String) {} }

        // Puente de mensajes
        messageBridge = MessageBridge(context, networkService, messageRepo, conversationRepo, logger, notificationHelper)
        messageBridge.start()

        // Inicializar Premium y perfil del dispositivo
        PremiumManager.init()
        CoroutineScope(Dispatchers.IO).launch {
            DeviceProfile.initialize(context)
        }
    }
}
