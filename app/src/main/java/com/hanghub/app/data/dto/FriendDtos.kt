package com.hanghub.app.data.dto

import kotlinx.serialization.Serializable

/** An accepted friend — mirrors iOS `APIFriend`. */
@Serializable
data class FriendDto(
    val id: String,
    val name: String = "",
    val username: String? = null,
    val avatar: String = "",
    val avatarId: String? = null,
    val status: String = "offline",
    val auraPoints: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationUpdatedAt: String? = null,
)

/** Returned by /users/search and /users/check-contacts — mirrors iOS `APISearchUser`. */
@Serializable
data class SearchUserDto(
    val id: String,
    val name: String = "",
    val username: String = "",
    val avatar: String = "",
    val avatarId: String? = null,
    val friendshipStatus: String? = null,   // null | pending | accepted
    val friendshipId: String? = null,
)

/** Incoming friend request — mirrors iOS `APIFriendRequest`. */
@Serializable
data class FriendRequestDto(
    val id: String,
    val fromId: String = "",
    val fromName: String = "",
    val fromAvatar: String = "",
    val fromAvatarId: String? = null,
    val toId: String = "",
    val createdAt: String? = null,
    val note: String? = null,
    val mutualCount: Int? = null,
)

/** Outgoing (sent) friend request — mirrors iOS `APISentRequest`. */
@Serializable
data class SentRequestDto(
    val id: String,
    val toId: String = "",
    val toName: String = "",
    val toAvatar: String = "",
    val createdAt: String? = null,
)

// ── Wrapped list responses ───────────────────────────────────────────────────

@Serializable
data class FriendsResponse(val friends: List<FriendDto> = emptyList())

@Serializable
data class FriendRequestsResponse(val requests: List<FriendRequestDto> = emptyList())

@Serializable
data class SentRequestsResponse(val requests: List<SentRequestDto> = emptyList())

@Serializable
data class UsersResponse(val users: List<SearchUserDto> = emptyList())
