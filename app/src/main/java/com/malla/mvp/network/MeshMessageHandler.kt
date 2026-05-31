package com.malla.mvp.network

import android.util.Log
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.data.entity.MessageEntity
import kotlinx.coroutines.*
import java.util.UUID

object MeshMessageHandler {
    private const val TAG = "MeshMessageHandler"
    private var job: Job? = null

    fun start(appContext: android.content.Context) {
        if (job != null) return
        job = CoroutineScope(Dispatchers.IO).launch {
            NetworkService.messages.collect { meshMsg ->
                try {
                    val db = AppDatabase.getInstance(appContext)
                    if (db == null) {
                        Log.e(TAG, "Base de datos no disponible")
                        return@collect   // reemplaza el 'continue'
                    }
                    val conversationDao = db.conversationDao()
                    val messageDao = db.messageDao()

                    val conversationId = meshMsg.senderId
                    var conv = conversationDao.getConversationById(conversationId)
                    if (conv == null) {
                        conv = ConversationEntity(
                            id = conversationId,
                            title = "Peer ${conversationId.take(8)}",
                            lastMessage = meshMsg.content,
                            timestamp = System.currentTimeMillis()
                        )
                        conversationDao.insertConversation(conv)
                    } else {
                        conversationDao.updateLastMessage(conversationId, meshMsg.content, System.currentTimeMillis())
                    }

                    val msgEntity = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        content = meshMsg.content,
                        timestamp = meshMsg.timestamp,
                        isOwn = false
                    )
                    messageDao.insertMessage(msgEntity)
                    Log.d(TAG, "Mensaje guardado de ${meshMsg.senderId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando mensaje mesh", e)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
