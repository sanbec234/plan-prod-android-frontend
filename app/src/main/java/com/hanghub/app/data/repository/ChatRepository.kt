package com.hanghub.app.data.repository

import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.core.network.ApiService
import com.hanghub.app.core.network.discardValue
import com.hanghub.app.core.network.map
import com.hanghub.app.core.network.safeApiCall
import com.hanghub.app.data.dto.AddMembersRequest
import com.hanghub.app.data.dto.ChatDto
import com.hanghub.app.data.dto.CreateChatRequest
import com.hanghub.app.data.dto.MessageDto
import com.hanghub.app.data.dto.SendMessageRequest
import com.hanghub.app.data.dto.UpdateGroupRequest

/**
 * Chat + group operations — wraps `/chats`, `/groups`, and `/messages`.
 * Mirrors the iOS `ChatAPIService`.
 */
class ChatRepository(private val api: ApiService) {

    suspend fun getChats(): ApiResult<List<ChatDto>> =
        safeApiCall { api.getChats() }

    suspend fun getGroups(): ApiResult<List<ChatDto>> =
        safeApiCall { api.getGroups() }.map { it.groups }

    suspend fun getMessages(chatId: String): ApiResult<List<MessageDto>> =
        safeApiCall { api.getMessages(chatId) }

    suspend fun createChat(
        memberIds: List<String>,
        isGroup: Boolean = false,
        name: String? = null,
        emoji: String? = null,
    ): ApiResult<ChatDto> =
        safeApiCall { api.createChat(CreateChatRequest(memberIds, isGroup, name, emoji)) }

    suspend fun updateGroup(chatId: String, name: String, emoji: String): ApiResult<ChatDto> =
        safeApiCall { api.updateGroup(chatId, UpdateGroupRequest(name, emoji)) }

    suspend fun addMembers(chatId: String, memberIds: List<String>): ApiResult<ChatDto> =
        safeApiCall { api.addMembers(chatId, AddMembersRequest(memberIds)) }

    suspend fun removeMember(chatId: String, userId: String): ApiResult<Unit> =
        safeApiCall { api.removeMember(chatId, userId) }.discardValue()

    suspend fun sendMessage(chatId: String, text: String): ApiResult<Unit> =
        safeApiCall {
            api.sendMessage(SendMessageRequest(type = "text", content = text, chatId = chatId))
        }.discardValue()
}
