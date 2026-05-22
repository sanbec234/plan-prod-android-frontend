package com.hanghub.app.data

import com.hanghub.app.core.util.Dates
import com.hanghub.app.data.dto.ChatDto
import com.hanghub.app.data.dto.FriendDto
import com.hanghub.app.data.dto.MessageDto
import com.hanghub.app.data.dto.MessageEnvelopeDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
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

/**
 * Build the lightweight [HHChat] used in the chats list. For DMs the partner's
 * name/avatar may be absent from the chat payload's member list — fall back to
 * the loaded [friends] list, keyed by the other member's user id.
 */
fun ChatDto.toHHChat(currentUserId: String, friends: List<HHUser> = emptyList()): HHChat {
    val other = members.firstOrNull { it.userId != currentUserId }
    val friend = other?.let { o -> friends.firstOrNull { it.id == o.userId } }
    val last = messages.firstOrNull()
    return HHChat(
        id = id,
        userID = other?.userId ?: "",
        lastMessage = last?.content ?: "",
        time = Dates.shortLabel(Dates.parseMillis(last?.createdAt)),
        unread = 0,
        // DM chats are created with an empty `name`, so blank must fall
        // through to the partner's name rather than short-circuiting here.
        partnerName = name?.takeIf { it.isNotBlank() }
            ?: other?.user?.name?.takeIf { it.isNotBlank() }
            ?: friend?.name?.takeIf { it.isNotBlank() }
            ?: "Chat",
        partnerAvatar = other?.user?.avatarEmoji?.takeIf { it.isNotBlank() }
            ?: friend?.avatar?.takeIf { it.isNotBlank() }
            ?: "👤",
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

// ── Modern chat:* message envelope ──────────────────────────────────────────

private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.dbl(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

/** Decode the raw [MessageEnvelopeDto.payload] into a typed [MessagePayload]. */
fun MessageEnvelopeDto.toMessagePayload(): MessagePayload = when (type) {
    "text" -> MessagePayload.Text(payload.str("text") ?: "")
    "image" -> MessagePayload.Image(
        uploadUrl = payload.str("uploadUrl") ?: "",
        thumbnailUrl = payload.str("thumbnailUrl") ?: payload.str("uploadUrl") ?: "",
        width = payload.int("width") ?: 0,
        height = payload.int("height") ?: 0,
    )
    "location_pin" -> MessagePayload.LocationPin(
        lat = payload.dbl("lat") ?: 0.0,
        lng = payload.dbl("lng") ?: 0.0,
        placeName = payload.str("placeName") ?: "Location",
        address = payload.str("address"),
        deepLinkUrl = payload.str("deepLinkUrl") ?: "",
    )
    "plan_card" -> MessagePayload.PlanCard(
        planId = payload.str("planId") ?: "",
        title = payload.str("title") ?: "Hangout plan",
        venueName = payload.str("venueName"),
        planStatus = payload.str("planStatus") ?: "",
    )
    "system" -> MessagePayload.SystemNote(
        subtype = payload.str("subtype") ?: "",
        text = payload.str("text") ?: "",
    )
    else -> MessagePayload.Unknown
}

/** A display-ready one-line string for any payload type. */
fun MessagePayload.displayText(): String = when (this) {
    is MessagePayload.Text -> text
    is MessagePayload.Image -> "📷 Photo"
    is MessagePayload.LocationPin -> "📍 $placeName"
    is MessagePayload.PlanCard -> "📋 $title"
    is MessagePayload.SystemNote -> text
    MessagePayload.Unknown -> "Unsupported message"
}

/** Map a modern chat envelope into the UI [ChatMessage]. */
fun MessageEnvelopeDto.toChatMessage(currentUserId: String): ChatMessage {
    val parsed = toMessagePayload()
    return ChatMessage(
        from = if (senderId == currentUserId) "me" else senderId,
        text = if (deletedAt != null) "Message deleted" else parsed.displayText(),
        time = Dates.shortLabel(Dates.parseMillis(createdAt)),
        id = id,
        senderName = senderName,
        pending = false,
        seq = seq,
        payload = parsed,
    )
}

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
