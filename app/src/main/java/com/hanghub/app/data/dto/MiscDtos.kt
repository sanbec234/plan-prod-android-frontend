package com.hanghub.app.data.dto

import kotlinx.serialization.Serializable

// ── Notifications ────────────────────────────────────────────────────────────

@Serializable
data class NotificationDto(
    val id: String,
    val type: String = "",
    val title: String? = null,
    val body: String? = null,
    val read: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class NotificationsResponse(val notifications: List<NotificationDto> = emptyList())

// ── Aura ─────────────────────────────────────────────────────────────────────

@Serializable
data class AuraResponse(
    val auraPoints: Int = 0,
    val breakdown: Map<String, Int> = emptyMap(),
)
