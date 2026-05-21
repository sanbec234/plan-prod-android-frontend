package com.hanghub.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanghub.app.core.network.ApiError
import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.data.PendingAction
import com.hanghub.app.data.repository.ChatRepository
import com.hanghub.app.data.repository.PlanRepository
import com.hanghub.app.ui.state.AppStateViewModel
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * Plan mutations for the Hangouts tab — vote, RSVP, transition, delete, create.
 * Reads/writes the shared [AppStateViewModel]; the screen renders from that.
 */
class PlansViewModel(
    private val appState: AppStateViewModel,
    private val planRepository: PlanRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    var actionError by mutableStateOf<String?>(null)
        private set
    var isCreating by mutableStateOf(false)
        private set

    fun clearError() {
        actionError = null
    }

    /** Toggle the current user's vote for a place (optimistic). */
    fun toggleVote(planId: String, placeId: String) {
        val votes = appState.myVotes.toMutableMap()
        if (votes[planId] == placeId) votes.remove(planId) else votes[planId] = placeId
        appState.myVotes = votes
        viewModelScope.launch {
            when (val result = planRepository.vote(planId, placeId)) {
                is ApiResult.Success -> appState.refreshPlans()
                is ApiResult.Failure ->
                    if (result.error is ApiError.Network) {
                        appState.enqueuePendingAction(
                            PendingAction(UUID.randomUUID().toString(), "vote", planId, placeId = placeId)
                        )
                    } else {
                        fail(result.error)
                    }
            }
        }
    }

    /** RSVP with a frontend status ("yes" | "no" | "maybe") — optimistic. */
    fun updateRsvp(planId: String, status: String) {
        appState.applyOptimisticRsvp(planId, status)
        viewModelScope.launch {
            when (val result = planRepository.rsvp(planId, status)) {
                is ApiResult.Success -> appState.refreshPlans()
                is ApiResult.Failure ->
                    if (result.error is ApiError.Network) {
                        appState.enqueuePendingAction(
                            PendingAction(UUID.randomUUID().toString(), "rsvp", planId, status = status)
                        )
                    } else {
                        fail(result.error)
                    }
            }
        }
    }

    fun transition(planId: String, toState: String, finalPlaceId: String? = null) {
        viewModelScope.launch {
            when (val result = planRepository.transition(planId, toState, finalPlaceId)) {
                is ApiResult.Success -> appState.refreshPlans()
                is ApiResult.Failure -> fail(result.error)
            }
        }
    }

    fun deletePlan(planId: String) {
        appState.removePlanLocally(planId)
        viewModelScope.launch {
            when (val result = planRepository.deletePlan(planId)) {
                is ApiResult.Success -> appState.refreshPlans()
                is ApiResult.Failure -> {
                    fail(result.error)
                    appState.refreshPlans()  // revert optimistic removal
                }
            }
        }
    }

    /**
     * Create a plan: resolve the chat (a group's own chat, or a fresh DM with a
     * friend), POST the plan, then add the chosen places.
     */
    fun createPlan(
        companionId: String,
        companionIsGroup: Boolean,
        title: String,
        placeNames: List<String>,
        voteUntilMs: Long,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            isCreating = true
            actionError = null

            val chatId: String? = if (companionIsGroup) {
                companionId
            } else {
                when (val result = chatRepository.createChat(listOf(companionId))) {
                    is ApiResult.Success -> result.data.id
                    is ApiResult.Failure -> {
                        fail(result.error); null
                    }
                }
            }
            if (chatId == null) {
                isCreating = false
                onResult(false)
                return@launch
            }

            val now = System.currentTimeMillis()
            val addPlacesUntil = now + 5 * 60_000L
            val rsvpUntil = voteUntilMs + 60 * 60_000L
            when (val result =
                planRepository.createPlan(chatId, title, addPlacesUntil, voteUntilMs, rsvpUntil)) {
                is ApiResult.Success -> {
                    placeNames.forEach { name ->
                        planRepository.addPlace(result.data.id, name, 0.0, 0.0)
                    }
                    appState.refreshPlans()
                    appState.refreshChats()
                    isCreating = false
                    onResult(true)
                }
                is ApiResult.Failure -> {
                    fail(result.error)
                    isCreating = false
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
