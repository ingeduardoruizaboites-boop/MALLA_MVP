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
import com.malla.mvp.core.transport.SmsTransport
import com.malla.mvp.core.transport.FlashlightTransport
import com.malla.mvp.core.util.ILogger
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.MessageEntity
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.network.*
import com.malla.mvp.network.DhtService
import com.malla.mvp.network.SeedManager
import com.malla.mvp.network.MessageReceiver
import com.malla.mvp.network.UnifiedMessageRouter
import com.malla.mvp.network.ContactDiscoveryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object Injector {
    lateinit var messageBridge: MessageBridge
    lateinit var smsTransport: SmsTransport
    lateinit var flashlightTransport: FlashlightTransport
    val messageRepo: IMessageRepository get() = _messageRepo
    private lateinit var _messageRepo: IMessageRepository
    lateinit var networkService: INetworkService

    fun init(context: Context) {
        val db = AppDatabase.getInstance(context)
        if (db == null) {
            android.widget.Toast.makeText(context, "Base de datos no disponible. Funcionamiento limitado.", android.widget.Toast.LENGTH_LONG).show()
            // La app continúa sin BD
        }

        // Inicializar transporte Faro (luz/cámara)
        smsTransport = SmsTransport(context)
        smsTransport.start()
        flashlightTransport = FlashlightTransport(context)
        flashlightTransport.start()

        // Adaptador de red (NetworkService -> INetworkService)
        val networkService = object : INetworkService {
            override val connectionState: Flow<Boolean> = NetworkService.connectedClientsCount.map { it > 0 }
            override suspend fun sendMeshMessage(message: CoreMeshMessage): Result<Unit> {
                return try {
                    val nsMessage = com.malla.mvp.core.network.MeshMessage(
                        content = message.content,
                        senderId = message.senderId,
                        timestamp = message.timestamp,
                        type = 0
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
                        listener(CoreMeshMessage(
                            senderId = nsMsg.senderId,
                            content = nsMsg.content,
                            timestamp = nsMsg.timestamp,
                            type = 0
                        ))
                    }
                }
            }
            override fun removeMessageListener(listener: (CoreMeshMessage) -> Unit) {}
        }
        this.networkService = networkService

        // Repositorio de mensajes (Room)
        val messageRepo = object : IMessageRepository {
            private val fallbackMessages = MutableStateFlow<List<MessageData>>(emptyList())
            override fun observeMessages(conversationId: String): Flow<List<MessageData>> {
                return if (db != null) {
                    try {
                        db.messageDao().getMessagesForConversation(conversationId).map { entities ->
                            entities.map {
                                MessageData(it.id, it.conversationId, it.content, it.timestamp, it.isOwn)
                            }
                        }
                    } catch (e: Exception) {
                        flowOf(emptyList())
                    }
                } else {
                    fallbackMessages.map { list -> list.filter { it.conversationId == conversationId } }
                }
            }
            override suspend fun saveMessage(message: MessageData) {
                try {
                    if (db != null) {
                        db.messageDao().insertMessage(
                            MessageEntity(
                                id = message.id,
                                conversationId = message.conversationId,
                                content = message.content,
                                timestamp = message.timestamp,
                                isOwn = message.isOwn,
                                status = 1,
                                mediaUri = message.mediaUri,
                                expireAt = message.expireAt,
                                viewOnce = message.viewOnce
                            )
                        )
                        android.widget.Toast.makeText(
                            App.context,
                            "Mensaje guardado en BD",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        fallbackMessages.value = fallbackMessages.value + message
                        android.widget.Toast.makeText(
                            App.context,
                            "BD no disponible - guardado en memoria",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        App.context,
                        "Error al guardar: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            override suspend fun getLastMessage(conversationId: String): MessageData? = null
            override suspend fun updateMessageStatus(messageId: String, status: Int) {}
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun getUnreadMessages(conversationId: String): List<MessageData> = emptyList()
            override suspend fun getPendingMessages(): List<MessageData> = emptyList()
        }
        this._messageRepo = messageRepo

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
        messageBridge.onSendMessage = { contactId, text ->
            UnifiedMessageRouter.sendMessage(contactId, text)
        }
        messageBridge.start()
        MessageReceiver.start(context)
        messageBridge.onIncomingMessage = { msg ->
            val meshMsg = com.malla.mvp.core.network.MeshMessage(
                content = msg.content,
                senderId = msg.senderId,
                timestamp = msg.timestamp,
                type = 0
            )
            MessageReceiver.process(context, meshMsg)
        }

        // Inicializar Premium y perfil del dispositivo
        PremiumManager.init()
        DhtService.start()
        SeedManager.init(context)
        ContactDiscoveryManager.publishMyPresence()
        CoroutineScope(Dispatchers.IO).launch {
            DeviceProfile.initialize(context)
        }
    }
}
