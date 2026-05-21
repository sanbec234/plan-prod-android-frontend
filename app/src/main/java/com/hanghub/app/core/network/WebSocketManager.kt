package com.hanghub.app.core.network

import com.hanghub.app.core.Config
import com.hanghub.app.core.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Realtime events surfaced to the app from the chat WebSocket. */
sealed interface WsEvent {
    data class NewMessage(
        val chatId: String,
        val id: String,
        val senderId: String,
        val senderName: String,
        val text: String,
        val timestamp: String,
    ) : WsEvent

    data class Typing(val chatId: String, val userId: String, val isTyping: Boolean) : WsEvent
    data class PlanUpdated(val chatId: String) : WsEvent
    data class Connection(val connected: Boolean) : WsEvent
}

/**
 * Single WebSocket connection to the backend chat server. Mirrors the iOS
 * `WebSocketManager`: connects with the JWT as a query param, auto-reconnects
 * with exponential backoff, and surfaces incoming events as a [SharedFlow].
 */
class WebSocketManager(
    private val okHttpClient: OkHttpClient,
    private val tokenProvider: () -> String?,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<WsEvent> = _events

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val reconnectAttempt = AtomicInteger(0)
    @Volatile private var intentionallyClosed = false

    fun connect() {
        val token = tokenProvider() ?: return
        intentionallyClosed = false
        reconnectJob?.cancel()
        webSocket?.cancel()

        val request = Request.Builder()
            .url("${Config.WS_URL}?token=$token")
            .build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun disconnect() {
        intentionallyClosed = true
        reconnectJob?.cancel()
        webSocket?.close(1000, null)
        webSocket = null
    }

    fun joinChat(chatId: String) {
        send(buildString {
            append("{\"type\":\"join_chat\",\"chatId\":")
            append("\"").append(chatId).append("\"}")
        })
    }

    /** Send a chat message over the socket. Returns the client message id. */
    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String,
    ): String {
        val msgId = UUID.randomUUID().toString()
        val payload = JsonValue.obj(
            "type" to JsonValue.str("message_sent"),
            "chatId" to JsonValue.str(chatId),
            "message" to JsonValue.obj(
                "id" to JsonValue.str(msgId),
                "clientMessageId" to JsonValue.str(msgId),
                "senderId" to JsonValue.str(senderId),
                "senderName" to JsonValue.str(senderName),
                "messageType" to JsonValue.str("text"),
                "text" to JsonValue.str(text),
                "timestamp" to JsonValue.str(Dates.nowIso()),
            ),
        )
        send(payload)
        return msgId
    }

    fun sendTyping(chatId: String, userId: String, isTyping: Boolean) {
        send(
            JsonValue.obj(
                "type" to JsonValue.str("typing"),
                "chatId" to JsonValue.str(chatId),
                "userId" to JsonValue.str(userId),
                "isTyping" to JsonValue.bool(isTyping),
            )
        )
    }

    private fun send(raw: String) {
        webSocket?.send(raw)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempt.set(0)
            _events.tryEmit(WsEvent.Connection(true))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleIncoming(text)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _events.tryEmit(WsEvent.Connection(false))
            if (!intentionallyClosed) scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _events.tryEmit(WsEvent.Connection(false))
            if (!intentionallyClosed) scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        val attempt = reconnectAttempt.getAndIncrement()
        val delaySecs = minOf(Math.pow(2.0, attempt.toDouble()), 30.0)
        reconnectJob = scope.launch {
            delay((delaySecs * 1000).toLong())
            if (!intentionallyClosed) connect()
        }
    }

    private fun handleIncoming(text: String) {
        val obj = try {
            json.parseToJsonElement(text) as? JsonObject ?: return
        } catch (_: Exception) {
            return
        }
        when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "message_sent", "receive_message" -> {
                val chatId = obj.str("chatId") ?: return
                val msg = obj["message"]?.jsonObject ?: return
                _events.tryEmit(
                    WsEvent.NewMessage(
                        chatId = chatId,
                        id = msg.str("id") ?: UUID.randomUUID().toString(),
                        senderId = msg.str("senderId") ?: "",
                        senderName = msg.str("senderName") ?: "",
                        text = msg.str("text") ?: "",
                        timestamp = msg.str("timestamp") ?: Dates.nowIso(),
                    )
                )
            }
            "chat:message" -> {
                val msg = obj["message"]?.jsonObject ?: return
                val roomId = msg.str("roomId") ?: return
                val payloadText = msg["payload"]?.jsonObject?.str("text") ?: ""
                _events.tryEmit(
                    WsEvent.NewMessage(
                        chatId = roomId,
                        id = msg.str("id") ?: UUID.randomUUID().toString(),
                        senderId = msg.str("senderId") ?: "",
                        senderName = msg.str("senderName") ?: "",
                        text = payloadText,
                        timestamp = msg.str("createdAt") ?: Dates.nowIso(),
                    )
                )
            }
            "typing" -> {
                val chatId = obj.str("chatId") ?: return
                val userId = obj.str("userId") ?: return
                val isTyping = obj["isTyping"]?.jsonPrimitive?.booleanOrNull ?: false
                _events.tryEmit(WsEvent.Typing(chatId, userId, isTyping))
            }
            "plan_updated", "plan_state_changed", "plan_locked", "vote_cast", "rsvp_updated" -> {
                obj.str("chatId")?.let { _events.tryEmit(WsEvent.PlanUpdated(it)) }
            }
        }
    }

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}

/** Tiny JSON builder to avoid manual string concatenation for outgoing frames. */
private object JsonValue {
    fun str(v: String): String = "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    fun bool(v: Boolean): String = v.toString()
    fun obj(vararg pairs: Pair<String, String>): String =
        pairs.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" }
}
