package com.hanghub.app.data.dto

import kotlinx.serialization.Serializable

/** Backend SerializedPlan — mirrors iOS `APIPlan`. */
@Serializable
data class PlanDto(
    val id: String,
    val chatId: String = "",
    val createdBy: String = "",
    val state: String = "spot_drop",   // spot_drop | pulse_vote | locked_in | cancelled
    val addPlacesUntil: String? = null,
    val voteUntil: String? = null,
    val rsvpUntil: String? = null,
    val finalPlaceId: String? = null,
    val title: String? = null,
    val winningPlace: PlanPlaceDto? = null,
    val places: List<PlanPlaceDto> = emptyList(),
    val rsvps: List<RsvpDto> = emptyList(),
)

@Serializable
data class PlanPlaceDto(
    val id: String,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addedBy: String? = null,
    val createdAt: String? = null,
    val voteCount: Int = 0,
)

@Serializable
data class RsvpDto(
    val userId: String,
    val status: String = "pending",    // going | not_going | pending
    val updatedAt: String? = null,
)

/** POST /plans response. */
@Serializable
data class PlanCreateResponse(
    val plan: PlanDto,
    val message: MessageDto? = null,
)
