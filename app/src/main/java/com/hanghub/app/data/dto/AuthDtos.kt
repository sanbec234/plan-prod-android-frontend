package com.hanghub.app.data.dto

import kotlinx.serialization.Serializable

// ── /auth/google response ───────────────────────────────────────────────────

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto,
    val isNewUser: Boolean? = null,
)

/** Backend user shape — mirrors iOS `APIUser`. */
@Serializable
data class UserDto(
    val id: String,
    val name: String = "",
    val username: String = "",
    val avatar: String = "",        // avatarEmoji from backend
    val avatarId: String? = null,   // "1"–"116"; null when no image asset chosen
    val status: String = "free",
    val auraPoints: Int = 0,
    val email: String? = null,
)

// ── /api/v1/profile/* ────────────────────────────────────────────────────────

@Serializable
data class ProfileDto(
    val userId: String,
    val displayName: String = "",
    val handle: String = "",
    val avatarId: String? = null,
    val profileUpdatedAt: String? = null,
)
