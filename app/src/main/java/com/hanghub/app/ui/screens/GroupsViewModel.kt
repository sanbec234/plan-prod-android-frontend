package com.hanghub.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanghub.app.core.network.ApiError
import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.data.repository.ChatRepository
import com.hanghub.app.ui.state.AppStateViewModel
import kotlinx.coroutines.launch

/**
 * Group operations for the Circles → Groups tab: create, rename, add members,
 * and leave. The group list itself lives in the shared [AppStateViewModel].
 */
class GroupsViewModel(
    private val appState: AppStateViewModel,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    var isWorking by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        actionError = null
    }

    fun createGroup(
        name: String,
        emoji: String,
        memberIds: List<String>,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            isWorking = true
            actionError = null
            val result = chatRepository.createChat(
                memberIds = memberIds,
                isGroup = true,
                name = name.trim().ifEmpty { "New group" },
                emoji = emoji,
            )
            isWorking = false
            when (result) {
                is ApiResult.Success -> {
                    appState.refreshGroups()
                    onResult(true)
                }
                is ApiResult.Failure -> {
                    fail(result.error)
                    onResult(false)
                }
            }
        }
    }

    fun renameGroup(chatId: String, name: String, emoji: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isWorking = true
            val result = chatRepository.updateGroup(chatId, name.trim(), emoji)
            isWorking = false
            when (result) {
                is ApiResult.Success -> {
                    appState.refreshGroups()
                    onResult(true)
                }
                is ApiResult.Failure -> {
                    fail(result.error)
                    onResult(false)
                }
            }
        }
    }

    fun addMembers(chatId: String, memberIds: List<String>, onResult: (Boolean) -> Unit) {
        if (memberIds.isEmpty()) {
            onResult(true)
            return
        }
        viewModelScope.launch {
            isWorking = true
            val result = chatRepository.addMembers(chatId, memberIds)
            isWorking = false
            when (result) {
                is ApiResult.Success -> {
                    appState.refreshGroups()
                    onResult(true)
                }
                is ApiResult.Failure -> {
                    fail(result.error)
                    onResult(false)
                }
            }
        }
    }

    fun leaveGroup(chatId: String, currentUserId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isWorking = true
            val result = chatRepository.removeMember(chatId, currentUserId)
            isWorking = false
            when (result) {
                is ApiResult.Success -> {
                    appState.refreshGroups()
                    onResult(true)
                }
                is ApiResult.Failure -> {
                    fail(result.error)
                    onResult(false)
                }
            }
        }
    }

    private fun fail(error: ApiError) {
        if (error is ApiError.Unauthorized) appState.onUnauthorized()
        actionError = error.message
    }
}
