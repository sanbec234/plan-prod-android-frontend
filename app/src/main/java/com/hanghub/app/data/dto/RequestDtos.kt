package com.hanghub.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Auth ─────────────────────────────────────────────────────────────────────

@Serializable
data class GoogleAuthRequest(
    @SerialName("id_token") val idToken: String,
    val name: String? = null,
    val email: String? = null,
)

// ── Plans ────────────────────────────────────────────────────────────────────

@Serializable
data class CreatePlanRequest(
    val chatId: String,
    val title: String? = null,
    val addPlacesUntil: String,
    val voteUntil: String,
    val rsvpUntil: String,
)

@Serializable
data class AddPlaceRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class VoteRequest(val placeId: String)

@Serializable
data class RsvpRequest(val status: String)   // going | not_going | pending

@Serializable
data class TransitionRequest(
    val toState: String,
    val finalPlaceId: String? = null,
)

// ── Chats ────────────────────────────────────────────────────────────────────

@Serializable
data class CreateChatRequest(
    val memberIds: List<String>,
    val isGroup: Boolean = false,
    val name: String? = null,
    val emoji: String? = null,
)

@Serializable
data class UpdateGroupRequest(val name: String, val emoji: String)

@Serializable
data class AddMembersRequest(val memberIds: List<String>)

@Serializable
data class SendMessageRequest(
    val type: String,
    val content: String,
    val chatId: String,
)

// ── Friends ──────────────────────────────────────────────────────────────────

@Serializable
data class FriendRequestBody(val toId: String, val note: String? = null)

@Serializable
data class RequestIdBody(val requestId: String)

@Serializable
data class CheckContactsRequest(val emails: List<String>)

// ── Profile / user ───────────────────────────────────────────────────────────

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val handle: String? = null,
    val avatarId: String? = null,
)

@Serializable
data class LocationRequest(val latitude: Double, val longitude: Double)

// ── Notifications ────────────────────────────────────────────────────────────

@Serializable
data class DeviceTokenRequest(
    val token: String,
    val environment: String = "production",
    val platform: String = "android",
)

@Serializable
data class NotificationReadRequest(val id: String)
