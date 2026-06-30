package com.malla.mvp.network

import android.content.Context
import android.util.Log
import com.malla.mvp.core.data.ConversationData
import com.malla.mvp.core.data.IConversationRepository
import com.malla.mvp.core.data.IMessageRepository
import com.malla.mvp.core.data.MessageData
import com.malla.mvp.core.network.INetworkService
import com.malla.mvp.core.network.MeshMessage as CoreMeshMessage
import com.malla.mvp.core.notification.INotificationHelper
import com.malla.mvp.core.util.ILogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore

class MessageBridge(
    private val context: Context,
    private val networkService: INetworkService,
    private val messageRepository: IMessageRepository,
    private val conversationRepository: IConversationRepository,
    private val logger: ILogger,
    private val notificationHelper: INotificationHelper,
    private val forwardLimit: Int = 5
) {
    private val TAG = "MessageBridge"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Callback para enrutamiento unificado
    var onSendMessage: ((String, String) -> Unit)? = null
    var isPremium: Boolean = false

    // Cinta transportadora: cola y semáforo para forward
    private val forwardSemaphore = Semaphore(forwardLimit)
    private val forwardQueue = ConcurrentLinkedQueue<CoreMeshMessage>()
    private var forwardJob: Job? = null

    private val _newMessageCount = MutableStateFlow(0)
    val newMessageCount: StateFlow<Int> = _newMessageCount.asStateFlow()

    fun start() {
        Log.d(TAG, "[MB:LIFE] Iniciando MessageBridge (forwardLimit=$forwardLimit)")
        networkService.addMessageListener { meshMessage ->
            scope.launch { handleIncomingMessage(meshMessage) }
        }
        startForwardLoop()
    }

    fun stop() {
        Log.d(TAG, "[MB:LIFE] Deteniendo MessageBridge")
        forwardJob?.cancel()
    }

    // ── Envío de mensajes ──
    fun sendMessageTo(contactId: String, text: String) {
        val callback = onSendMessage
        if (callback != null) {
            callback(contactId, text)
        } else {
            scope.launch {
                try {
                    val msg = CoreMeshMessage(senderId = "Yo", content = text, type = 0)
                    networkService.sendMeshMessage(msg)
                } catch (e: Exception) {
                    Log.e(TAG, "[MB:SEND] Error enviando: ${e.message}")
                }
            }
        }
    }

    // ── Recepción de mensajes ──
    private suspend fun handleIncomingMessage(meshMessage: CoreMeshMessage) {
        val convId = meshMessage.senderId

        // Crear conversación si no existe
        val existingConv = conversationRepository.getConversation(convId)
        if (existingConv == null) {
            val newConv = ConversationData(
                id = convId,
                name = meshMessage.senderId.take(15),
                lastMessage = meshMessage.content.take(30),
                timestamp = meshMessage.timestamp
            )
            conversationRepository.upsertConversation(newConv)
        }

        // Guardar mensaje
        val msg = MessageData(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            content = meshMessage.content,
            timestamp = meshMessage.timestamp,
            isOwn = false
        )
        messageRepository.saveMessage(msg)
        _newMessageCount.value++
        notificationHelper.showNotification("Nuevo mensaje de $convId", meshMessage.content)
    }

    // ── Cinta transportadora (forward) ──
    private fun startForwardLoop() {
        forwardJob = scope.launch {
            while (isActive) {
                val msg = forwardQueue.poll()
                if (msg != null) {
                    forwardSemaphore.acquire()
                    launch {
                        try {
                            networkService.sendMeshMessage(msg)
                            Log.d(TAG, "[MB:FWD] Mensaje reenviado")
                        } catch (e: Exception) {
                            Log.e(TAG, "[MB:FWD] Error reenviando: ${e.message}")
                        } finally {
                            forwardSemaphore.release()
                        }
                    }
                } else {
                    delay(50)
                }
            }
        }
    }

    fun scheduleForward(message: CoreMeshMessage) {
        if (isPremium && forwardSemaphore.tryAcquire()) {
            scope.launch {
                try {
                    networkService.sendMeshMessage(message)
                } catch (e: Exception) {
                    Log.e(TAG, "[MB:FWD] Premium: error reenviando: ${e.message}")
                } finally {
                    forwardSemaphore.release()
                }
            }
        } else if (forwardSemaphore.availablePermits() > 0 || forwardQueue.size < 20) {
            forwardQueue.add(message)
        } else {
            Log.w(TAG, "[MB:FWD] Cola llena, descartando mensaje")
        }
    }

    // ── WebRTC ──
    fun sendWebRtcSignal(contactId: String, signal: String) {
        scope.launch {
            val msg = CoreMeshMessage(senderId = "Yo", content = signal, type = 3)
            networkService.sendMeshMessage(msg)
        }
    }

    // ── Marcar como leído ──
    fun markConversationAsRead(conversationId: String) {
        scope.launch {
            messageRepository.markConversationAsRead(conversationId)
        }
    }
}
