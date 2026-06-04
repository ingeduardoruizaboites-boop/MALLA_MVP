package com.malla.mvp.network

import android.content.Context
import android.util.Log
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.MessageEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

/**
 * Puente entre la red mesh y la base de datos local.
 * Observa los mensajes entrantes de NetworkService y los guarda en Room.
 * De esta forma, ChatScreen los ve automáticamente al observar MessageDao.
 *
 * Tags de log: [MB:LIFE], [MB:MSG]
 */
object MessageBridge {
    private const val TAG = "MessageBridge"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var collectionJob: Job? = null

    /**
     * Número de mensajes nuevos recibidos (para notificaciones futuras).
     */
    private val _newMessageCount = MutableStateFlow(0)
    val newMessageCount: StateFlow<Int> = _newMessageCount.asStateFlow()

    /**
     * Inicia el puente. Debe llamarse cuando la app está en funcionamiento.
     */
    fun start(context: Context) {
        Log.d(TAG, "[MB:LIFE] Iniciando MessageBridge")
        collectionJob?.cancel()

        val db = AppDatabase.getInstance(context)
        if (db == null) {
            Log.e(TAG, "[MB:LIFE] No se pudo obtener base de datos")
            return
        }
        val messageDao = db.messageDao()

        collectionJob = scope.launch {
            NetworkService.messages.collect { meshMessage ->
                // Solo guardamos mensajes entrantes (no los propios)
                if (meshMessage.type == "delete") {
                    // Mensaje de eliminación: borrar localmente
                    Log.d(TAG, "[MB:MSG] Recibida solicitud de eliminación: ${meshMessage.content}")
                    messageDao.deleteMessage(meshMessage.content)
                } else {
                    // Mensaje normal o cita
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = meshMessage.senderId.take(8), // Usamos parte del ID del remitente
                        content = meshMessage.content,
                        timestamp = meshMessage.timestamp,
                        isOwn = false,
                        status = 1, // Recibido
                        quotedMessageId = meshMessage.quotedMessageId,
                        quotedMessageContent = meshMessage.quotedMessageContent,
                        reaction = null,
                        expireAt = null,
                        mediaUri = null,
                        viewOnce = false
                    )
                    messageDao.insertMessage(msg)
                    _newMessageCount.value++
                    Log.d(TAG, "[MB:MSG] Mensaje guardado en Room: ${meshMessage.content.take(30)}...")
                }
            }
        }
    }

    /**
     * Detiene el puente.
     */
    fun stop() {
        Log.d(TAG, "[MB:LIFE] Deteniendo MessageBridge")
        collectionJob?.cancel()
        collectionJob = null
    }
}
