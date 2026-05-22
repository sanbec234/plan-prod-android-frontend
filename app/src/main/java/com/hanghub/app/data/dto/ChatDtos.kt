package com.hanghub.app.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A chat (DM when isGroup=false, group otherwise) — mirrors iOS `APIChat`. */
@Serializable
data class ChatDto(
    val id: String,
    val isGroup: Boolean = false,
    val name: String? = null,
    val emoji: String? = null,
    val members: List<ChatMemberDto> = emptyList(),
    val messages: List<MessageDto> = emptyList(),  // last message in list endpoint
)

@Serializable
data class ChatMemberDto(
    val userId: String,
    val role: String = "scout",
    val user: ChatUserDto? = null,
)

@Serializable
data class ChatUserDto(
    val id: String,
    val name: String = "",
    val avatarEmoji: String = "",
    val avatarId: String? = null,
)

/** A chat message — mirrors iOS `APIMessage`. */
@Serializable
data class MessageDto(
    val id: String,
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "text",      // text | plan | system
    val content: String? = null,
    val planId: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class GroupsResponse(val groups: List<ChatDto> = emptyList())

// ── Modern chat:* protocol ───────────────────────────────────────────────────

/**
 * Canonical server message envelope — the wire shape of the modern chat
 * protocol (`chat:message` events and `GET /api/v1/rooms/:id/messages`).
 * Mirrors the backend `MessageEnvelope` in modules/chat/chat.types.ts.
 *
 * [payload] is kept as a raw [JsonObject] because its shape is discriminated
 * by [type]; it is decoded into a typed model by the mapper layer.
 */
@Serializable
data class MessageEnvelopeDto(
    val id: String,
    val clientMessageId: String? = null,
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "text",            // text | image | location_pin | plan_card | system
    val payload: JsonObject = JsonObject(emptyMap()),
    val seq: Long = 0,
    val createdAt: String? = null,
    val editedAt: String? = null,
    val deletedAt: String? = null,
)

/** Cursor-paginated message page — `GET /api/v1/rooms/:roomId/messages`. */
@Serializable
data class MessagesPageDto(
    val messages: List<MessageEnvelopeDto> = emptyList(),
    val nextCursor: Long? = null,
)
