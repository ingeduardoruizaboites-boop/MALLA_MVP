package com.malla.mvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malla.mvp.core.data.MessageData
import com.malla.mvp.di.Injector
import com.malla.mvp.network.CascadeRouter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    // Lista local como fuente inmediata (para UI reactiva)
    private val _messages = MutableStateFlow<List<MessageData>>(emptyList())
    val messages: StateFlow<List<MessageData>> = _messages.asStateFlow()

    private val _conversationId = MutableStateFlow<String?>(null)
    private var lastLoadTime = 0L
    private var observerJob: kotlinx.coroutines.Job? = null

    fun loadConversation(convId: String) {
        _conversationId.value = convId
        lastLoadTime = System.currentTimeMillis()

        // Cancelar suscripción anterior
        observerJob?.cancel()
        // Suscribirse al repositorio para mantener la lista actualizada
        observerJob = viewModelScope.launch {
            try {
                Injector.messageRepo.observeMessages(convId).collect { list ->
                    _messages.value = list
                }
            } catch (e: Exception) {
                // Si falla, mantenemos la lista local actual
            }
        }
    }

    fun isMessageNew(timestamp: Long): Boolean = timestamp > lastLoadTime

    fun sendMessage(text: String, mediaUri: String? = null) {
        val convId = _conversationId.value ?: return

        // Crear el mensaje
        val msg = MessageData(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            content = text,
            timestamp = System.currentTimeMillis(),
            isOwn = true,
            mediaUri = mediaUri
        )

        // Agregar inmediatamente a la lista local (el usuario lo ve al instante)
        _messages.value = _messages.value + msg

        // Intentar guardar en repositorio (persistencia)
        viewModelScope.launch {
            try {
                Injector.messageRepo.saveMessage(msg)
                // No es necesario recargar, porque la lista local ya lo tiene
            } catch (e: Exception) {
                // Si falla, el mensaje queda en memoria (al menos se ve)
            }
        }

        // Intentar enviar por red (mejor esfuerzo)
        viewModelScope.launch {
            try {
                CascadeRouter.sendMessage(convId, text)
            } catch (_: Exception) {}
        }
    }

    fun sendZumbido() {
        val convId = _conversationId.value ?: return
        viewModelScope.launch {
            try { CascadeRouter.sendMessage(convId, "Zumbido!", type = "zumbido") } catch (_: Exception) {}
        }
    }
}
