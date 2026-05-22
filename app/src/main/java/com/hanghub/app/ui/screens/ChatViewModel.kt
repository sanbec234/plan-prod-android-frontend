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
import com.hanghub.app.data.dto.MessageEnvelopeDto
import com.hanghub.app.data.repository.ChatRepository
import com.hanghub.app.data.toChatMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * Drives a single open chat thread on the modern `chat:*` protocol: loads
 * history over REST, joins the room over the WebSocket with a replay cursor,
 * sends messages with optimistic echo, and reconciles them against the
 * server's `chat:message` / `chat:message_ack`.
 */
class ChatViewModel(
    private val roomId: String,
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

    /** Highest server `seq` seen — the replay cursor for (re)joins. */
    private var lastSeenSeq = 0L
    private var typingJob: Job? = null
    private var joined = false

    init {
        observeRealtime()
        loadHistoryThenJoin()
    }

    private fun loadHistoryThenJoin() {
        viewModelScope.launch {
            isLoading = messages.isEmpty()
            error = null
            when (val result = chatRepository.getRoomMessages(roomId)) {
                is ApiResult.Success -> {
                    val ordered = result.data.messages.sortedBy { it.seq }
                    messages = ordered.map { it.toChatMessage(currentUserId) }
                    lastSeenSeq = ordered.maxOfOrNull { it.seq } ?: 0L
                }
                is ApiResult.Failure -> error = result.error.message
            }
            isLoading = false
            // Join after history so replay only backfills the gap since
            // [lastSeenSeq]. If the history fetch failed, lastSeenSeq is 0 and
            // the join replay backfills the recent history over the socket.
            webSocketManager.joinRoom(roomId, lastSeenSeq)
            joined = true
        }
    }

    private fun observeRealtime() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                when (event) {
                    is WsEvent.IncomingMessage ->
                        if (event.message.roomId == roomId) applyIncoming(event.message)
                    is WsEvent.MessageAck ->
                        messages = messages.map {
                            if (it.id == event.messageId) {
                                it.copy(pending = false, seq = event.seq)
                            } else {
                                it
                            }
                        }
                    is WsEvent.MessageDeleted ->
                        if (event.roomId == roomId) {
                            messages = messages.filterNot { it.id == event.messageId }
                        }
                    is WsEvent.Typing ->
                        if (event.roomId == roomId && event.userId != currentUserId) {
                            partnerTyping = event.isTyping
                        }
                    is WsEvent.Connection ->
                        // Re-join after a reconnect to replay anything missed
                        // while the socket was down.
                        if (event.connected && joined) {
                            webSocketManager.joinRoom(roomId, lastSeenSeq)
                        }
                    else -> Unit
                }
            }
        }
    }

    private fun applyIncoming(envelope: MessageEnvelopeDto) {
        if (envelope.seq > lastSeenSeq) lastSeenSeq = envelope.seq
        val incoming = envelope.toChatMessage(currentUserId)
        val idx = messages.indexOfFirst { it.id == incoming.id }
        messages = if (idx >= 0) {
            // Reconcile our optimistic copy, or dedupe a replayed message.
            messages.toMutableList().also { it[idx] = incoming }
        } else {
            messages + incoming
        }
        if (envelope.senderId != currentUserId) partnerTyping = false
    }

    fun onDraftChange(text: String) {
        draft = text
        webSocketManager.sendTyping(roomId, text.isNotEmpty())
        typingJob?.cancel()
        if (text.isNotEmpty()) {
            typingJob = viewModelScope.launch {
                delay(3000)
                webSocketManager.sendTyping(roomId, false)
            }
        }
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty()) return
        draft = ""
        typingJob?.cancel()
        webSocketManager.sendTyping(roomId, false)

        // Pre-generate the id so the optimistic row, the server echo, and the
        // ack all share it — the send is idempotent and reconciles in place.
        val id = UUID.randomUUID().toString()
        webSocketManager.sendChatMessage(
            roomId = roomId,
            messageType = "text",
            payload = buildJsonObject { put("text", text) },
            id = id,
            clientMessageId = id,
        )

        messages = messages + ChatMessage(
            from = "me",
            text = text,
            time = "now",
            id = id,
            pending = true,
        )
    }
}
