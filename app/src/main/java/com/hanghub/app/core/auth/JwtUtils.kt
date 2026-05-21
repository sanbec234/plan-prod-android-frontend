package com.hanghub.app.core.auth

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Minimal JWT inspection — reads the `exp` claim so the app can refresh the
 * session proactively, before the 90-day backend token actually expires.
 * No signature verification; that is the server's job.
 */
object JwtUtils {

    private const val REFRESH_WINDOW_MS = 7L * 24 * 60 * 60 * 1000  // 7 days

    /** Epoch millis at which [token] expires, or null if it cannot be read. */
    fun expiryMillis(token: String?): Long? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            )
            Json.parseToJsonElement(payload).jsonObject["exp"]
                ?.jsonPrimitive?.longOrNull
                ?.times(1000)
        } catch (_: Exception) {
            null
        }
    }

    /** True when the token is already expired. */
    fun isExpired(token: String?): Boolean {
        val exp = expiryMillis(token) ?: return false
        return exp <= System.currentTimeMillis()
    }

    /** True when the token expires within the refresh window (or is already expired). */
    fun isExpiringSoon(token: String?): Boolean {
        val exp = expiryMillis(token) ?: return false
        return exp - System.currentTimeMillis() < REFRESH_WINDOW_MS
    }
}
