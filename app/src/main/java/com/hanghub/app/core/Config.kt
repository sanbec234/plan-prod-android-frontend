package com.hanghub.app.core

/**
 * Single source of truth for the backend location. The same server that the
 * iOS app and backend already use. Nothing else in the app may hardcode a URL.
 */
object Config {

    /** REST base URL. Must end with a trailing slash for Retrofit. */
    const val BASE_URL: String = "https://plan-prod-0wom.onrender.com/"

    /** WebSocket origin, derived from [BASE_URL]. */
    val WS_URL: String =
        BASE_URL.trimEnd('/')
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
}
