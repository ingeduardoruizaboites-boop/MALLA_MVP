package com.malla.mvp.network

import android.content.Context
import android.util.Log
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.data.entity.MessageEntity
import com.malla.mvp.events.MallaEventBus
import kotlinx.coroutines.*
import java.util.UUID

/**
 * Maneja los mensajes entrantes de la red mesh.
 * Aplica anti-replay, filtro Bloom, guarda en Room y emite eventos al bus.
 *
 * Tags de log: [MMH:LIFE], [MMH:MSG], [MMH:REPLAY], [MMH:ERR]
 */
object MeshMessageHandler {
    private const val TAG = "MeshMessageHandler"
    private var job: Job? = null
    private val bloomFilter = BloomFilter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(appContext: Context) {
        if (job != null) return
        bloomFilter.startAutoRotation()
        Log.d(TAG, "[MMH:LIFE] MeshMessageHandler iniciado con Bloom Filter + Anti-Replay")

        job = scope.launch {
            NetworkService.messages.collect { meshMsg ->
                try {
                    val db = AppDatabase.getInstance(appContext)
                    if (db == null) {
                        Log.e(TAG, "[MMH:ERR] Base de datos no disponible")
                        return@collect
                    }

                    // ── Anti-Replay ──────────────────────────────
                    val messageId = "${meshMsg.senderId}_${meshMsg.timestamp}_${meshMsg.content.hashCode()}"
                    if (!ReplayProtection.validate(messageId, meshMsg.timestamp)) {
                        Log.w(TAG, "[MMH:REPLAY] Mensaje duplicado o fuera de ventana: $messageId")
                        return@collect
                    }

                    // ── Bloom Filter ──────────────────────────────
                    if (bloomFilter.mightContain(messageId)) {
                        Log.w(TAG, "[MMH:REPLAY] Bloom Filter: posible duplicado de $messageId")
                        return@collect
                    }
                    bloomFilter.add(messageId)

                    // ── Guardar en Room ───────────────────────────
                    val conversationDao = db.conversationDao()
                    val messageDao = db.messageDao()
                    val conversationId = meshMsg.senderId

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
                        status = 1,
                        quotedMessageId = meshMsg.quotedMessageId,
                        quotedMessageContent = meshMsg.quotedMessageContent
                    )
                    messageDao.insertMessage(msgEntity)

                    // ── Emitir al bus de eventos ──────────────────
                    MallaEventBus.messageReceived.emit(meshMsg)

                    Log.d(TAG, "[MMH:MSG] Mensaje guardado de ${meshMsg.senderId}: ${meshMsg.content.take(30)}...")
                } catch (e: Exception) {
                    Log.e(TAG, "[MMH:ERR] Error procesando mensaje mesh: ${e.message}", e)
                }
            }
        }
    }

    fun stop() {
        Log.d(TAG, "[MMH:LIFE] MeshMessageHandler detenido")
        bloomFilter.stopAutoRotation()
        job?.cancel()
        job = null
    }
}
