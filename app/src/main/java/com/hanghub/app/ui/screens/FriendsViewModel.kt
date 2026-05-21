package com.hanghub.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanghub.app.core.network.ApiError
import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.data.dto.FriendRequestDto
import com.hanghub.app.data.dto.SearchUserDto
import com.hanghub.app.data.repository.FriendsRepository
import com.hanghub.app.ui.state.AppStateViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Friends tab logic — incoming requests, user search, and the
 * send / accept / decline / remove actions. Reads the friends list from the
 * shared [AppStateViewModel].
 */
class FriendsViewModel(
    private val appState: AppStateViewModel,
    private val friendsRepository: FriendsRepository,
) : ViewModel() {

    var requests by mutableStateOf<List<FriendRequestDto>>(emptyList())
        private set
    var isLoadingRequests by mutableStateOf(false)
        private set

    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<SearchUserDto>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set

    /** User ids with an in-flight or completed outgoing request this session. */
    var sentTo by mutableStateOf<Set<String>>(emptySet())
        private set

    var actionError by mutableStateOf<String?>(null)
        private set

    private var searchJob: Job? = null

    init {
        loadRequests()
    }

    fun clearError() {
        actionError = null
    }

    fun loadRequests() {
        viewModelScope.launch {
            isLoadingRequests = true
            when (val result = friendsRepository.getFriendRequests()) {
                is ApiResult.Success -> requests = result.data
                is ApiResult.Failure -> fail(result.error)
            }
            isLoadingRequests = false
        }
    }

    /** Debounced search — fires 350 ms after the user stops typing. */
    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            isSearching = true
            when (val result = friendsRepository.searchUsers(query.trim())) {
                is ApiResult.Success -> searchResults = result.data
                is ApiResult.Failure -> fail(result.error)
            }
            isSearching = false
        }
    }

    fun sendRequest(userId: String) {
        sentTo = sentTo + userId
        viewModelScope.launch {
            when (val result = friendsRepository.sendFriendRequest(userId)) {
                is ApiResult.Success -> Unit
                is ApiResult.Failure -> {
                    sentTo = sentTo - userId
                    fail(result.error)
                }
            }
        }
    }

    fun accept(requestId: String) {
        requests = requests.filterNot { it.id == requestId }
        viewModelScope.launch {
            when (val result = friendsRepository.acceptFriendRequest(requestId)) {
                is ApiResult.Success -> appState.refreshFriends()
                is ApiResult.Failure -> {
                    fail(result.error)
                    loadRequests()
                }
            }
        }
    }

    fun decline(requestId: String) {
        requests = requests.filterNot { it.id == requestId }
        viewModelScope.launch {
            when (val result = friendsRepository.declineFriendRequest(requestId)) {
                is ApiResult.Success -> Unit
                is ApiResult.Failure -> {
                    fail(result.error)
                    loadRequests()
                }
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            when (val result = friendsRepository.removeFriend(friendId)) {
                is ApiResult.Success -> appState.refreshFriends()
                is ApiResult.Failure -> fail(result.error)
            }
        }
    }

    private fun fail(error: ApiError) {
        if (error is ApiError.Unauthorized) appState.onUnauthorized()
        actionError = error.message
    }
}
