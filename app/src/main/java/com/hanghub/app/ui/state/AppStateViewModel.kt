package com.hanghub.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanghub.app.core.AppPreferences
import com.hanghub.app.core.network.ApiError
import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.core.network.NetworkMonitor
import com.hanghub.app.core.network.WebSocketManager
import com.hanghub.app.core.network.WsEvent
import com.hanghub.app.core.storage.LocalCache
import com.hanghub.app.data.PendingAction
import com.hanghub.app.data.dto.ChatDto
import com.hanghub.app.data.dto.UserDto
import com.hanghub.app.data.repository.AuthRepository
import com.hanghub.app.data.repository.ChatRepository
import com.hanghub.app.data.repository.FriendsRepository
import com.hanghub.app.data.repository.PlanRepository
import com.hanghub.app.data.HHChat
import com.hanghub.app.data.HHPlan
import com.hanghub.app.data.HHUser
import com.hanghub.app.data.toHHChat
import com.hanghub.app.data.toHHPlan
import com.hanghub.app.data.toHHUser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

/**
 * Single source of truth for the app — the Android counterpart of the iOS
 * `AppStore`. Activity-scoped and shared with every screen through
 * [LocalAppState]. Holds the auth slice and the live backend data
 * (plans / friends / chats / groups).
 */
class AppStateViewModel(
    private val authRepository: AuthRepository,
    private val planRepository: PlanRepository,
    private val friendsRepository: FriendsRepository,
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager,
    private val appPreferences: AppPreferences,
    private val localCache: LocalCache,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private var realtimeStarted = false

    /** Theme preference: "system" | "light" | "dark". */
    var themeMode by mutableStateOf(appPreferences.themeMode)
        private set

    fun applyThemeMode(mode: String) {
        themeMode = mode
        appPreferences.themeMode = mode
    }

    /** Patch the cached current user after a profile edit. */
    fun updateCurrentUserProfile(displayName: String, handle: String) {
        currentUser = currentUser?.copy(name = displayName, username = handle)
    }

    // ── Auth ────────────────────────────────────────────────────────────────
    var isAuthenticated by mutableStateOf(authRepository.isAuthenticated)
        private set
    var currentUser by mutableStateOf(authRepository.currentUser)
        private set

    val currentUserId: String get() = currentUser?.id ?: ""

    // ── Live data ───────────────────────────────────────────────────────────
    var plans by mutableStateOf<List<HHPlan>>(emptyList())
        private set
    var friends by mutableStateOf<List<HHUser>>(emptyList())
        private set
    var chats by mutableStateOf<List<HHChat>>(emptyList())
        private set
    var groups by mutableStateOf<List<ChatDto>>(emptyList())
        private set

    /** planId → placeId the current user voted for (optimistic local tracking). */
    var myVotes by mutableStateOf<Map<String, String>>(emptyMap())

    // ── Loading / error ─────────────────────────────────────────────────────
    var isLoadingPlans by mutableStateOf(false)
        private set
    var isLoadingCircles by mutableStateOf(false)
        private set
    /** Last load error (non-auth). Null when the most recent load succeeded. */
    var loadError by mutableStateOf<String?>(null)
        private set
    /** True when the last failure was a connectivity error. */
    var isOffline by mutableStateOf(false)
        private set

    init {
        // Watch connectivity — refresh + drain the offline queue on reconnect.
        viewModelScope.launch {
            var wasOnline = networkMonitor.online.value
            networkMonitor.online.collect { online ->
                isOffline = !online
                if (online && !wasOnline && isAuthenticated) {
                    webSocketManager.connect()
                    loadInitialData()
                    drainPendingActions()
                }
                wasOnline = online
            }
        }
        if (isAuthenticated) {
            // Render instantly from the last cached snapshot, then refresh.
            viewModelScope.launch { restoreFromCache() }
            if (authRepository.needsRefresh) {
                viewModelScope.launch {
                    val refreshed = authRepository.refreshSession()
                    if (refreshed) currentUser = authRepository.currentUser
                    else if (authRepository.isSessionExpired) { clearSession(); return@launch }
                    loadInitialData()
                    drainPendingActions()
                    connectRealtime()
                }
            } else {
                loadInitialData()
                drainPendingActions()
                connectRealtime()
            }
        }
    }

    /** Open the chat WebSocket and route live events into shared state. */
    private fun connectRealtime() {
        if (realtimeStarted) return
        realtimeStarted = true
        webSocketManager.connect()
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                when (event) {
                    is WsEvent.NewMessage -> refreshChats()
                    is WsEvent.PlanUpdated -> refreshPlans()
                    is WsEvent.Typing -> setChatTyping(event.chatId, event.isTyping)
                    is WsEvent.Connection -> { /* connection state — no-op here */ }
                }
            }
        }
    }

    private fun setChatTyping(chatId: String, typing: Boolean) {
        chats = chats.map { if (it.id == chatId) it.copy(isTyping = typing) else it }
    }

    // ── Auth actions ────────────────────────────────────────────────────────

    fun onSignedIn(user: UserDto) {
        currentUser = user
        isAuthenticated = true
        loadInitialData()
        connectRealtime()
    }

    /** One silent refresh attempt after a 401; sign out if it fails. */
    fun onUnauthorized() {
        viewModelScope.launch {
            if (authRepository.refreshSession()) {
                currentUser = authRepository.currentUser
            } else {
                clearSession()
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
        clearSession()
    }

    private fun clearSession() {
        webSocketManager.disconnect()
        viewModelScope.launch { localCache.clear() }
        realtimeStarted = false
        isAuthenticated = false
        currentUser = null
        plans = emptyList()
        friends = emptyList()
        chats = emptyList()
        groups = emptyList()
        myVotes = emptyMap()
        loadError = null
        isOffline = false
    }

    // ── Data loading ────────────────────────────────────────────────────────

    /** Refresh plans, friends, chats and groups in parallel. */
    fun loadInitialData() {
        viewModelScope.launch {
            isLoadingCircles = true
            if (plans.isEmpty()) isLoadingPlans = true
            loadError = null
            isOffline = false
            listOf(
                async { loadPlans() },
                async { loadFriends() },
                async { loadChats() },
                async { loadGroups() },
            ).awaitAll()
            isLoadingCircles = false
            isLoadingPlans = false
        }
    }

    fun refreshPlans() {
        viewModelScope.launch {
            if (plans.isEmpty()) isLoadingPlans = true
            loadPlans()
            isLoadingPlans = false
        }
    }

    fun refreshFriends() {
        viewModelScope.launch { loadFriends() }
    }

    fun refreshChats() {
        viewModelScope.launch { loadChats() }
    }

    fun refreshGroups() {
        viewModelScope.launch { loadGroups() }
    }

    /** Replace the plans list — used by feature ViewModels for optimistic updates. */
    fun setPlansLocally(updated: List<HHPlan>) {
        plans = updated
    }

    /** Optimistically patch the current user's RSVP on a plan before the server confirms. */
    fun applyOptimisticRsvp(planId: String, frontendStatus: String) {
        plans = plans.map { plan ->
            if (plan.id == planId) {
                plan.copy(rsvp = plan.rsvp.toMutableMap().apply { put("me", frontendStatus) })
            } else {
                plan
            }
        }
    }

    /** Remove a plan locally — optimistic delete. */
    fun removePlanLocally(planId: String) {
        plans = plans.filterNot { it.id == planId }
    }

    fun clearLoadError() {
        loadError = null
    }

    private suspend fun loadPlans() {
        when (val result = planRepository.getPlans()) {
            is ApiResult.Success -> {
                plans = result.data.mapNotNull { it.toHHPlan(currentUserId) }
                localCache.savePlans(result.data)
            }
            is ApiResult.Failure -> handleError(result.error)
        }
    }

    private suspend fun loadFriends() {
        when (val result = friendsRepository.getFriends()) {
            is ApiResult.Success -> {
                friends = result.data.map { it.toHHUser() }
                localCache.saveFriends(result.data)
            }
            is ApiResult.Failure -> handleError(result.error)
        }
    }

    private suspend fun loadChats() {
        when (val result = chatRepository.getChats()) {
            is ApiResult.Success -> {
                chats = result.data
                    .filter { !it.isGroup && it.id.isNotBlank() }
                    .map { it.toHHChat(currentUserId) }
                    .distinctBy { it.id }
                localCache.saveChats(result.data)
            }
            is ApiResult.Failure -> handleError(result.error)
        }
    }

    private suspend fun loadGroups() {
        when (val result = chatRepository.getGroups()) {
            is ApiResult.Success -> {
                groups = result.data.filter { it.id.isNotBlank() }.distinctBy { it.id }
                localCache.saveGroups(result.data)
            }
            is ApiResult.Failure -> handleError(result.error)
        }
    }

    // ── Offline cache + action queue ─────────────────────────────────────────

    /** Populate state from the last on-disk snapshot (only when still empty). */
    private suspend fun restoreFromCache() {
        if (plans.isEmpty()) {
            localCache.loadPlans()?.let { cached ->
                plans = cached.mapNotNull { it.toHHPlan(currentUserId) }
            }
        }
        if (friends.isEmpty()) {
            localCache.loadFriends()?.let { cached -> friends = cached.map { it.toHHUser() } }
        }
        if (chats.isEmpty()) {
            localCache.loadChats()?.let { cached ->
                chats = cached.filter { !it.isGroup && it.id.isNotBlank() }
                    .map { it.toHHChat(currentUserId) }
                    .distinctBy { it.id }
            }
        }
        if (groups.isEmpty()) {
            localCache.loadGroups()?.let { cached ->
                groups = cached.filter { it.id.isNotBlank() }.distinctBy { it.id }
            }
        }
    }

    /** Queue a vote/RSVP that failed because the device is offline. */
    fun enqueuePendingAction(action: PendingAction) {
        viewModelScope.launch { localCache.enqueueAction(action) }
    }

    /** Flush queued offline votes/RSVPs — called on app start and on reconnect. */
    fun drainPendingActions() {
        viewModelScope.launch {
            val actions = localCache.pendingActions()
            if (actions.isEmpty()) return@launch
            var anySucceeded = false
            for (action in actions) {
                val result = when (action.type) {
                    "vote" -> action.placeId?.let { planRepository.vote(action.planId, it) }
                    "rsvp" -> action.status?.let { planRepository.rsvp(action.planId, it) }
                    else -> null
                }
                if (result is ApiResult.Success) {
                    localCache.removeAction(action.id)
                    anySucceeded = true
                }
            }
            if (anySucceeded) loadPlans()
        }
    }

    private fun handleError(error: ApiError) {
        when (error) {
            is ApiError.Unauthorized -> onUnauthorized()
            is ApiError.Network -> {
                isOffline = true
                loadError = error.message
            }
            else -> loadError = error.message
        }
    }
}

/** Provides the activity-scoped [AppStateViewModel] to the whole Composable tree. */
val LocalAppState = staticCompositionLocalOf<AppStateViewModel> {
    error("LocalAppState not provided")
}
