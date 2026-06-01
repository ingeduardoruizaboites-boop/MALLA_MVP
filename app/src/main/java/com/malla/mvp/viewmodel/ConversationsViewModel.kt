package com.malla.mvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malla.mvp.data.repository.ConversationRepository
import com.malla.mvp.data.repository.ConversationWithLastMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val conversations: List<ConversationWithLastMessage> = emptyList(),
    val filteredConversations: List<ConversationWithLastMessage> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class ConversationsViewModel(
    private val repository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.insertInitialDemoDataIfNeeded()
            repository.observeAll().collect { convs ->
                _uiState.update { currentState ->
                    currentState.copy(
                        conversations = convs,
                        filteredConversations = filterConversations(convs, currentState.searchQuery),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = query,
                filteredConversations = filterConversations(currentState.conversations, query)
            )
        }
    }

    private fun filterConversations(
        list: List<ConversationWithLastMessage>,
        query: String
    ): List<ConversationWithLastMessage> {
        if (query.isBlank()) return list
        val q = query.lowercase()
        return list.filter {
            it.conversation.title.lowercase().contains(q) ||
            it.lastMessage?.content?.lowercase()?.contains(q) == true
        }
    }
}
