package com.malla.mvp.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malla.mvp.core.data.MessageData
import com.malla.mvp.di.Injector
import com.malla.mvp.network.CascadeRouter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    // Lista local como fuente de verdad principal
    private val _messages = MutableStateFlow<List<MessageData>>(emptyList())
    val messages: StateFlow<List<MessageData>> = _messages.asStateFlow()
    private val _conversationId = MutableStateFlow<String?>(null)
    private var lastLoadTime = 0L

    init {
        // Confirmar creación del ViewModel
        try {
            val app = Injector.messageRepo
            // Si no falla, estamos bien
        } catch (e: Exception) {
            // No se pudo obtener el repo, usaremos solo lista local
        }
    }

    fun loadConversation(convId: String) {
        _conversationId.value = convId
        lastLoadTime = System.currentTimeMillis()

        // Intentar cargar desde el repositorio, si falla, iniciamos vacío
        viewModelScope.launch {
            try {
                Injector.messageRepo.observeMessages(convId).collect { list ->
                    _messages.value = list
                }
            } catch (e: Exception) {
                _messages.value = emptyList()
            }
        }
    }

    fun isMessageNew(timestamp: Long): Boolean = timestamp > lastLoadTime

    fun sendMessage(text: String) {
        val convId = _conversationId.value
        if (convId == null) return

        // Crear el mensaje
        val msg = MessageData(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            content = text,
            timestamp = System.currentTimeMillis(),
            isOwn = true
        )

        // Agregar inmediatamente a la lista local (prioridad máxima)
        _messages.value = _messages.value + msg

        // Luego intentar guardar en repositorio (mejor esfuerzo)
        viewModelScope.launch {
            try {
                Injector.messageRepo.saveMessage(msg)
            } catch (e: Exception) {
                // No importa, ya está en la lista local
            }
        }

        // Intentar enviar por red (mejor esfuerzo)
        viewModelScope.launch {
            try {
                CascadeRouter.sendMessage(convId, text)
            } catch (e: Exception) {
                // No importa, ya está en la lista local
            }
        }
    }

    fun sendZumbido() {
        val convId = _conversationId.value ?: return
        viewModelScope.launch {
            try {
                CascadeRouter.sendMessage(convId, "Zumbido!", type = "zumbido")
            } catch (e: Exception) {
                // No importa
            }
        }
    }
}
