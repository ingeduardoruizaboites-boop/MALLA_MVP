package com.malla.mvp.network

import android.content.Context
import android.util.Log
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.data.entity.MessageEntity
import com.malla.mvp.di.Injector
import com.malla.mvp.core.network.MeshMessage
import com.malla.mvp.events.MallaEventBus
import kotlinx.coroutines.*
import java.util.UUID

object MessageReceiver {
    private const val TAG = "MessageReceiver"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bloomFilter = BloomFilter()
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        bloomFilter.startAutoRotation()
        LogBuffer.add(TAG, "MessageReceiver iniciado")

        // Mensajes de la red mesh (TCP/WebRTC/Internet)
        scope.launch {
            NetworkService.messages.collect { meshMsg ->
                process(context, meshMsg)
            }
        }

        // Mensajes SMS entrantes
        scope.launch {
            Injector.smsTransport.incomingMessages.collect { raw ->
                val parts = raw.split("|", limit = 2)
                val sender = parts.getOrElse(0) { "unknown" }
                val body = parts.getOrElse(1) { raw }
                val meshMsg = com.malla.mvp.core.network.MeshMessage(
                    content = body,
                    senderId = sender,
                    timestamp = System.currentTimeMillis(),
                    type = 0
                )
                process(context, meshMsg)
            }
        }
    }

    // Método público para que MessageBridge delegue
    fun process(context: Context, meshMsg: MeshMessage) {
        scope.launch {
            processInternal(context, meshMsg)
        }
    }

    private suspend fun processInternal(context: Context, meshMsg: MeshMessage) {
        try {
            val db = AppDatabase.getInstance(context) ?: return
            val messageId = "${meshMsg.senderId}_${meshMsg.timestamp}_${meshMsg.content.hashCode()}"

            if (!ReplayProtection.validate(messageId, meshMsg.timestamp)) {
                Log.w(TAG, "Duplicado: $messageId")
                return
            }
            if (bloomFilter.mightContain(messageId)) {
                Log.w(TAG, "Bloom: posible duplicado $messageId")
                return
            }
            bloomFilter.add(messageId)

            val conversationId = meshMsg.senderId
            val conversationDao = db.conversationDao()
            val messageDao = db.messageDao()

            var conv = conversationDao.getConversationById(conversationId)
            if (conv == null) {
                conv = ConversationEntity(
                    id = conversationId,
                    title = "Peer ${conversationId.take(8)}",
                    lastMessage = meshMsg.content.take(30),
                    timestamp = meshMsg.timestamp,
                    unreadCount = 1
                )
                conversationDao.insertConversation(conv)
            } else {
                conversationDao.updateLastMessage(
                    conversationId,
                    meshMsg.content.take(30),
                    meshMsg.timestamp
                )
            }

            val msgEntity = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                content = meshMsg.content,
                timestamp = meshMsg.timestamp,
                isOwn = false,
                status = 1
            )
            messageDao.insertMessage(msgEntity)

            MallaEventBus.messageReceived.emit(meshMsg)
            if (meshMsg.type == 4) {
                MallaEventBus.zumbidoReceived.emit(meshMsg)
            }
            LogBuffer.add(TAG, "Mensaje guardado de ${meshMsg.senderId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
        }
    }

    fun stop() {
        bloomFilter.stopAutoRotation()
        started = false
    }
}
