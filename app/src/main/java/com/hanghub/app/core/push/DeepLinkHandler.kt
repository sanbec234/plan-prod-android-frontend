package com.hanghub.app.core.push

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Where a tapped notification asks the app to navigate. */
sealed interface DeepLinkTarget {
    data class Chat(val chatId: String) : DeepLinkTarget
    data class Plan(val planId: String) : DeepLinkTarget
}

/**
 * Single source of truth for "where should the app navigate next?" — the
 * Android counterpart of the iOS DeepLinkHandler. Notification taps land here
 * (via intent extras); the Composable tree observes [pending] and consumes it.
 */
object DeepLinkHandler {

    var pending by mutableStateOf<DeepLinkTarget?>(null)
        private set

    /** Parse a launch/notification intent into a navigation target. */
    fun handleIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val chatId = extras.getString("chatId")
        val planId = extras.getString("planId")
        pending = when {
            !chatId.isNullOrBlank() -> DeepLinkTarget.Chat(chatId)
            !planId.isNullOrBlank() -> DeepLinkTarget.Plan(planId)
            else -> pending
        }
    }

    /** Take and clear the pending target. */
    fun consume(): DeepLinkTarget? {
        val target = pending
        pending = null
        return target
    }
}
