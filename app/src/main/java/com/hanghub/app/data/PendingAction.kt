package com.hanghub.app.data

import kotlinx.serialization.Serializable

/**
 * A vote or RSVP composed while offline, persisted until connectivity returns.
 * Mirrors the iOS offline action queue.
 */
@Serializable
data class PendingAction(
    val id: String,
    val type: String,            // "vote" | "rsvp"
    val planId: String,
    val placeId: String? = null, // for "vote"
    val status: String? = null,  // for "rsvp" — frontend value
)
