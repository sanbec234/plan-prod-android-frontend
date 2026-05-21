package com.hanghub.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.core.network.WebSocketManager
import com.hanghub.app.core.network.WsEvent
import com.hanghub.app.data.ChatMessage
import com.hanghub.app.data.repository.ChatRepository
import com.hanghub.app.data.toChatMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Drives a single open chat thread: message history, sending, the typing
 * indicator, and live inbound messages over the WebSocket.
 */
class ChatViewModel(
    private val chatId: String,
    private val currentUserId: String,
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager,
) : ViewModel() {

    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var draft by mutableStateOf("")
        private set
    var partnerTyping by mutableStateOf(false)
        private set

    private var typingJob: Job? = null

    init {
        webSocketManager.joinChat(chatId)
        loadMessages()
        observeRealtime()
    }

    fun loadMessages() {
        viewModelScope.launch {
            isLoading = messages.isEmpty()
            error = null
            when (val result = chatRepository.getMessages(chatId)) {
                is ApiResult.Success ->
                    messages = result.data.map { it.toChatMessage(currentUserId) }
                is ApiResult.Failure -> error = result.error.message
            }
            isLoading = false
        }
    }

    private fun observeRealtime() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                when (event) {
                    is WsEvent.NewMessage ->
                        // Only inbound messages from others — own sends reload via REST.
                        if (event.chatId == chatId && event.senderId != currentUserId) {
                            if (messages.none { it.id == event.id }) {
                                messages = messages + ChatMessage(
                                    from = event.senderId,
                                    text = event.text,
                                    time = "now",
                                    id = event.id,
                                    senderName = event.senderName,
                                )
                            }
                            partnerTyping = false
                        }
                    is WsEvent.Typing ->
                        if (event.chatId == chatId && event.userId != currentUserId) {
                            partnerTyping = event.isTyping
                        }
                    else -> Unit
                }
            }
        }
    }

    fun onDraftChange(text: String) {
        draft = text
        webSocketManager.sendTyping(chatId, currentUserId, text.isNotEmpty())
        typingJob?.cancel()
        if (text.isNotEmpty()) {
            typingJob = viewModelScope.launch {
                delay(3000)
                webSocketManager.sendTyping(chatId, currentUserId, false)
            }
        }
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty()) return
        draft = ""
        typingJob?.cancel()
        webSocketManager.sendTyping(chatId, currentUserId, false)

        // Optimistic — replaced by the server list once the send confirms.
        messages = messages + ChatMessage(
            from = "me",
            text = text,
            time = "now",
            id = "local-${System.currentTimeMillis()}",
            pending = true,
        )

        viewModelScope.launch {
            when (val result = chatRepository.sendMessage(chatId, text)) {
                is ApiResult.Success -> loadMessages()
                is ApiResult.Failure -> error = result.error.message
            }
        }
    }
}
