package com.malla.mvp.network

import android.content.Context
import android.util.Log
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.data.entity.MessageEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

/**
 * Puente entre la red mesh y la base de datos local.
 * Observa los mensajes entrantes de NetworkService y los guarda en Room.
 * Crea automáticamente una conversación para cada peer nuevo.
 *
 * Tags de log: [MB:LIFE], [MB:MSG], [MB:CONV]
 */
object MessageBridge {
    private const val TAG = "MessageBridge"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var collectionJob: Job? = null

    private val _newMessageCount = MutableStateFlow(0)
    val newMessageCount: StateFlow<Int> = _newMessageCount.asStateFlow()

    fun start(context: Context) {
        Log.d(TAG, "[MB:LIFE] Iniciando MessageBridge")
        collectionJob?.cancel()

        val db = AppDatabase.getInstance(context)
        if (db == null) {
            Log.e(TAG, "[MB:LIFE] No se pudo obtener base de datos")
            return
        }
        val messageDao = db.messageDao()
        val conversationDao = db.conversationDao()

        collectionJob = scope.launch {
            NetworkService.messages.collect { meshMessage ->
                val convId = meshMessage.senderId

                if (meshMessage.type == "delete") {
                    messageDao.deleteMessage(meshMessage.content)
                    return@collect
                }

                // Crear conversación si no existe
                val existingConv = conversationDao.getConversationById(convId)
                if (existingConv == null) {
                    val newConv = ConversationEntity(
                        id = convId,
                        title = meshMessage.senderId.take(15),
                        lastMessage = meshMessage.content.take(30),
                        timestamp = meshMessage.timestamp,
                        unreadCount = 1
                    )
                    conversationDao.insertConversation(newConv)
                    Log.d(TAG, "[MB:CONV] Nueva conversación creada para $convId")
                } else {
                    conversationDao.updateLastMessage(convId, meshMessage.content.take(30), meshMessage.timestamp)
                }

                val msg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    content = meshMessage.content,
                    timestamp = meshMessage.timestamp,
                    isOwn = false,
                    status = 1,
                    quotedMessageId = meshMessage.quotedMessageId,
                    quotedMessageContent = meshMessage.quotedMessageContent,
                    reaction = null,
                    expireAt = null,
                    mediaUri = null,
                    viewOnce = false
                )
                messageDao.insertMessage(msg)
                _newMessageCount.value++
                Log.d(TAG, "[MB:MSG] Mensaje guardado en $convId: ${meshMessage.content.take(30)}...")
            }
        }
    }

    fun stop() {
        Log.d(TAG, "[MB:LIFE] Deteniendo MessageBridge")
        collectionJob?.cancel()
        collectionJob = null
    }
}
