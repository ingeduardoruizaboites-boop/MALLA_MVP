package com.malla.mvp.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.MessageEntity
import com.malla.mvp.data.entity.PollEntity
import com.malla.mvp.data.entity.PollOptionEntity
import com.malla.mvp.network.MeshMessage
import com.malla.mvp.network.NetworkService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel para la pantalla de chat.
 * Orquesta la carga de mensajes desde Room, el envío de mensajes mesh,
 * las encuestas, y el estado efímero.
 *
 * Referencia: Arquitectura V3.0 — Sprint 1.4
 */
class MeshChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    // Estado de la conversación activa
    private val _conversationId = MutableStateFlow<String?>(null)
    val conversationId: StateFlow<String?> = _conversationId.asStateFlow()

    val messages: StateFlow<List<MessageEntity>> = _conversationId
        .flatMapLatest { convId ->
            if (convId != null && db != null) {
                db.messageDao().getMessagesForConversation(convId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Encuestas
    private val _polls = MutableStateFlow<List<PollEntity>>(emptyList())
    val polls: StateFlow<List<PollEntity>> = _polls.asStateFlow()

    private val _optionsMap = MutableStateFlow<Map<String, List<PollOptionEntity>>>(emptyMap())
    val optionsMap: StateFlow<Map<String, List<PollOptionEntity>>> = _optionsMap.asStateFlow()

    // Texto de entrada y estado de grabación
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    /**
     * Inicia la carga de la conversación indicada.
     */
    fun loadConversation(convId: String) {
        _conversationId.value = convId
        loadPolls(convId)
    }

    private fun loadPolls(convId: String) {
        viewModelScope.launch {
            db?.pollDao()?.getPollsForGroup(convId)?.collect { pollList ->
                _polls.value = pollList
                pollList.forEach { poll ->
                    db.pollDao().getOptionsForPoll(poll.id).collect { options ->
                        _optionsMap.value = _optionsMap.value + (poll.id to options)
                    }
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun startRecording() { _isRecording.value = true }
    fun stopRecording() { _isRecording.value = false }

    /**
     * Envía un mensaje de texto (con posible cita).
     */
    fun sendMessage(
        text: String,
        quotedMessageId: String? = null,
        quotedMessageContent: String? = null,
        expireAt: Long? = null,
        viewOnce: Boolean = false,
        mediaUri: String? = null
    ) {
        val convId = _conversationId.value ?: return
        viewModelScope.launch {
            val msg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                content = text.ifBlank { "📷 Imagen" },
                isOwn = true,
                expireAt = expireAt,
                mediaUri = mediaUri,
                viewOnce = viewOnce,
                quotedMessageId = quotedMessageId,
                quotedMessageContent = quotedMessageContent
            )
            db?.messageDao()?.insertMessage(msg)
            NetworkService.sendMessage(
                MeshMessage(
                    content = msg.content,
                    senderId = "self",
                    timestamp = System.currentTimeMillis(),
                    quotedMessageId = quotedMessageId,
                    quotedMessageContent = quotedMessageContent
                )
            )
            _inputText.value = ""
        }
    }

    /**
     * Vota en una encuesta (demo: suma 1 voto a la primera opción).
     */
    fun votePoll(optionId: String, pollId: String) {
        viewModelScope.launch {
            db?.pollDao()?.incrementVoteCount(optionId, 1)
            // Actualizar el mapa local
            val currentOptions = _optionsMap.value[pollId] ?: return@launch
            val updated = currentOptions.map { opt ->
                if (opt.id == optionId) opt.copy(voteCount = opt.voteCount + 1) else opt
            }
            _optionsMap.value = _optionsMap.value + (pollId to updated)
        }
    }

    fun createPoll(question: String, options: List<String>) {
        val convId = _conversationId.value ?: return
        viewModelScope.launch {
            val pollId = UUID.randomUUID().toString()
            db?.pollDao()?.insertPoll(PollEntity(id = pollId, groupId = convId, question = question, creatorId = "self"))
            options.forEach { text ->
                if (text.isNotBlank()) {
                    db?.pollDao()?.insertOption(PollOptionEntity(id = UUID.randomUUID().toString(), pollId = pollId, text = text))
                }
            }
            loadPolls(convId) // refrescar
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            db?.messageDao()?.deleteMessage(messageId)
        }
    }
}
