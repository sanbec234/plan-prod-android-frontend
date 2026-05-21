package com.hanghub.app.data.repository

import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.core.network.ApiService
import com.hanghub.app.core.network.discardValue
import com.hanghub.app.core.network.map
import com.hanghub.app.core.network.safeApiCall
import com.hanghub.app.core.util.Dates
import com.hanghub.app.data.dto.AddPlaceRequest
import com.hanghub.app.data.dto.CreatePlanRequest
import com.hanghub.app.data.dto.PlanDto
import com.hanghub.app.data.dto.RsvpRequest
import com.hanghub.app.data.dto.TransitionRequest
import com.hanghub.app.data.dto.VoteRequest
import com.hanghub.app.data.frontendRsvpToBackend

/**
 * Plan REST operations — wraps the plan endpoints. Mirrors the iOS
 * PlanAPIService. All calls return a typed [ApiResult].
 */
class PlanRepository(private val api: ApiService) {

    suspend fun getPlans(): ApiResult<List<PlanDto>> =
        safeApiCall { api.getPlans() }

    suspend fun createPlan(
        chatId: String,
        title: String?,
        addPlacesUntilMs: Long,
        voteUntilMs: Long,
        rsvpUntilMs: Long,
    ): ApiResult<PlanDto> =
        safeApiCall {
            api.createPlan(
                CreatePlanRequest(
                    chatId = chatId,
                    title = title,
                    addPlacesUntil = Dates.toIso(addPlacesUntilMs),
                    voteUntil = Dates.toIso(voteUntilMs),
                    rsvpUntil = Dates.toIso(rsvpUntilMs),
                )
            )
        }.map { it.plan }

    suspend fun addPlace(
        planId: String,
        name: String,
        latitude: Double,
        longitude: Double,
    ): ApiResult<Unit> =
        safeApiCall { api.addPlace(planId, AddPlaceRequest(name, latitude, longitude)) }
            .discardValue()

    /** Toggle the current user's vote for a place. */
    suspend fun vote(planId: String, placeId: String): ApiResult<Unit> =
        safeApiCall { api.vote(planId, VoteRequest(placeId)) }.discardValue()

    /** RSVP with a frontend status ("yes" | "no" | "maybe"). */
    suspend fun rsvp(planId: String, frontendStatus: String): ApiResult<Unit> =
        safeApiCall {
            api.rsvp(planId, RsvpRequest(frontendRsvpToBackend(frontendStatus)))
        }.discardValue()

    suspend fun transition(
        planId: String,
        toState: String,
        finalPlaceId: String? = null,
    ): ApiResult<Unit> =
        safeApiCall {
            api.transition(planId, TransitionRequest(toState, finalPlaceId))
        }.discardValue()

    suspend fun deletePlan(planId: String): ApiResult<Unit> =
        safeApiCall { api.deletePlan(planId) }.discardValue()
}
