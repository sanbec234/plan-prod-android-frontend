package com.hanghub.app.data

import com.hanghub.app.core.util.Dates
import com.hanghub.app.data.dto.ChatDto
import com.hanghub.app.data.dto.FriendDto
import com.hanghub.app.data.dto.MessageDto
import com.hanghub.app.data.dto.PlaceDto
import com.hanghub.app.data.dto.PlanDto
import com.hanghub.app.data.dto.PlanPlaceDto
import com.hanghub.app.data.dto.UserDto
import com.hanghub.app.ui.theme.HangVibe
import com.hanghub.app.ui.theme.UserStatus

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Backend DTO → UI model mapping
// Mirrors the `toHH*` extensions in the iOS Models.swift.
// ═══════════════════════════════════════════════════════════════════════════

/** Backend RSVP value → frontend display string. */
fun backendRsvpToFrontend(status: String): String = when (status) {
    "going" -> "yes"
    "not_going" -> "no"
    else -> "maybe"
}

/** Frontend display string → backend RSVP value. */
fun frontendRsvpToBackend(status: String): String = when (status) {
    "yes" -> "going"
    "no" -> "not_going"
    else -> "pending"
}

private fun userStatus(raw: String): UserStatus = when (raw.lowercase()) {
    "free" -> UserStatus.FREE
    "busy" -> UserStatus.BUSY
    else -> UserStatus.OFFLINE
}

private fun emojiForCategory(category: String): String {
    val l = category.lowercase()
    return when {
        l.contains("coffee") || l.contains("cafe") || l.contains("café") -> "☕"
        l.contains("restaurant") || l.contains("food") || l.contains("dining") -> "🍽️"
        l.contains("bar") || l.contains("wine") || l.contains("cocktail") -> "🍷"
        l.contains("park") || l.contains("outdoor") || l.contains("trail") -> "🌳"
        l.contains("music") || l.contains("concert") || l.contains("live") -> "🎵"
        l.contains("game") || l.contains("arcade") || l.contains("bowling") -> "🎮"
        l.contains("gym") || l.contains("fitness") -> "💪"
        l.contains("book") || l.contains("library") -> "📚"
        l.contains("museum") || l.contains("art") || l.contains("gallery") -> "🏛️"
        l.contains("cinema") || l.contains("movie") || l.contains("theatre") -> "🎬"
        else -> "📍"
    }
}

// ── Plan ────────────────────────────────────────────────────────────────────

/**
 * Convert a backend SerializedPlan into the UI [HHPlan]. The current user's id
 * is mapped to the legacy "me" key so existing screens keep working unchanged.
 * Returns null for cancelled plans (dropped from the UI).
 */
fun PlanDto.toHHPlan(currentUserId: String): HHPlan? {
    if (state == "cancelled") return null

    val uiState = if (state == "locked_in") PlanState.LOCKED else PlanState.VOTING
    // Backend returns only voteCount, not voter ids — synthesize ids so the
    // voting UI can render percentages.
    val votesMap = places.associate { p -> p.id to (0 until p.voteCount).map { "v$it" } }
    val rsvpMap = rsvps.associate { r ->
        (if (r.userId == currentUserId) "me" else r.userId) to backendRsvpToFrontend(r.status)
    }
    val voteMs = Dates.parseMillis(voteUntil)
    val hoursLeft = voteMs?.let { (it - System.currentTimeMillis()) / 3_600_000.0 } ?: 0.0
    val winning = if (finalPlaceId != null) winningPlace?.toHHPlace() else null

    return HHPlan(
        id = id,
        title = title?.takeIf { it.isNotBlank() } ?: "Hangout",
        vibe = HangVibe.CHILL,
        place = winning,
        time = Dates.relativeLabel(voteMs),
        inHrs = maxOf(0.0, hoursLeft),
        state = uiState,
        participantIDs = rsvps.map { if (it.userId == currentUserId) "me" else it.userId },
        rsvp = rsvpMap,
        options = places.map { it.toHHPlace() },
        votes = votesMap,
        chatId = chatId,
        rawBackendState = state,
        voteUntil = voteMs,
        rsvpUntil = Dates.parseMillis(rsvpUntil),
        hostId = if (createdBy == currentUserId) "me" else createdBy,
    )
}

fun PlanPlaceDto.toHHPlace(): HHPlace = HHPlace(
    id = id,
    name = name,
    category = "Place",
    emoji = "📍",
    vibe = HangVibe.CHILL,
    rating = 0.0,
    price = "$",
    dist = 0.0,
    match = voteCount * 10,
    friends = voteCount,
    hours = "",
)

// ── User / friend ───────────────────────────────────────────────────────────

fun FriendDto.toHHUser(): HHUser = HHUser(
    id = id,
    name = name,
    avatar = avatar.ifBlank { "👤" },
    status = userStatus(status),
    aura = auraPoints,
    avatarId = avatarId,
    username = username,
)

fun UserDto.toHHUser(): HHUser = HHUser(
    id = id,
    name = name,
    avatar = avatar.ifBlank { "👤" },
    status = UserStatus.FREE,
    aura = auraPoints,
    avatarId = avatarId,
    username = username.ifBlank { null },
)

// ── Chat ────────────────────────────────────────────────────────────────────

/** Build the lightweight [HHChat] used in the chats list. */
fun ChatDto.toHHChat(currentUserId: String): HHChat {
    val other = members.firstOrNull { it.userId != currentUserId }
    val last = messages.firstOrNull()
    return HHChat(
        id = id,
        userID = other?.userId ?: "",
        lastMessage = last?.content ?: "",
        time = Dates.shortLabel(Dates.parseMillis(last?.createdAt)),
        unread = 0,
        partnerName = name ?: other?.user?.name ?: "Chat",
        partnerAvatar = other?.user?.avatarEmoji?.ifBlank { "👤" } ?: "👤",
    )
}

/** Resolve the display name of a DM partner from a chat's member list. */
fun ChatDto.partnerName(currentUserId: String): String =
    members.firstOrNull { it.userId != currentUserId }?.user?.name ?: "Chat"

fun MessageDto.toChatMessage(currentUserId: String): ChatMessage = ChatMessage(
    from = if (senderId == currentUserId) "me" else senderId,
    text = when (type) {
        "plan" -> "📋 Shared a plan"
        else -> content ?: ""
    },
    time = Dates.shortLabel(Dates.parseMillis(createdAt)),
    id = id,
    senderName = senderName,
)

// ── Discovery place ─────────────────────────────────────────────────────────

fun PlaceDto.toHHPlace(): HHPlace {
    val priceStr = "$".repeat(maxOf(1, priceLevel ?: 2))
    val distMiles = (distanceMetres ?: 0) / 1609.0
    return HHPlace(
        id = id,
        name = name,
        category = category,
        emoji = emojiForCategory(category),
        vibe = HangVibe.CHILL,
        rating = rating,
        price = priceStr,
        dist = distMiles,
        match = 0,
        friends = 0,
        hours = if (isOpenNow == true) "Open now" else "Hours unknown",
    )
}
