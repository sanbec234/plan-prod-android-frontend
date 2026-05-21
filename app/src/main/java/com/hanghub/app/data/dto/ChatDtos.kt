package com.hanghub.app.data.dto

import kotlinx.serialization.Serializable

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
