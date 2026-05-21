package com.hanghub.app.core.storage

import android.content.Context
import com.hanghub.app.data.PendingAction
import com.hanghub.app.data.dto.ChatDto
import com.hanghub.app.data.dto.FriendDto
import com.hanghub.app.data.dto.PlanDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Lightweight on-disk cache — JSON files in the app's private storage. Lets
 * every tab render instantly from the last snapshot before (or without) a
 * network round-trip, and persists the offline action queue.
 *
 * This is the Android stand-in for the iOS SQLite ChatLocalStore; Room was not
 * added to keep the dependency set minimal.
 */
class LocalCache(context: Context) {

    private val dir: File = File(context.filesDir, "hh_cache").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun file(name: String) = File(dir, "$name.json")

    private suspend fun writeText(name: String, content: String) = withContext(Dispatchers.IO) {
        try {
            file(name).writeText(content)
        } catch (_: Exception) {
            // Cache writes are best-effort.
        }
    }

    private suspend fun readText(name: String): String? = withContext(Dispatchers.IO) {
        try {
            val f = file(name)
            if (f.exists()) f.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    // ── Circles data snapshots ───────────────────────────────────────────────

    suspend fun savePlans(plans: List<PlanDto>) =
        writeText(KEY_PLANS, json.encodeToString(ListSerializer(PlanDto.serializer()), plans))

    suspend fun loadPlans(): List<PlanDto>? =
        readText(KEY_PLANS)?.let { decode(it, PlanDto.serializer()) }

    suspend fun saveFriends(friends: List<FriendDto>) =
        writeText(KEY_FRIENDS, json.encodeToString(ListSerializer(FriendDto.serializer()), friends))

    suspend fun loadFriends(): List<FriendDto>? =
        readText(KEY_FRIENDS)?.let { decode(it, FriendDto.serializer()) }

    suspend fun saveChats(chats: List<ChatDto>) =
        writeText(KEY_CHATS, json.encodeToString(ListSerializer(ChatDto.serializer()), chats))

    suspend fun loadChats(): List<ChatDto>? =
        readText(KEY_CHATS)?.let { decode(it, ChatDto.serializer()) }

    suspend fun saveGroups(groups: List<ChatDto>) =
        writeText(KEY_GROUPS, json.encodeToString(ListSerializer(ChatDto.serializer()), groups))

    suspend fun loadGroups(): List<ChatDto>? =
        readText(KEY_GROUPS)?.let { decode(it, ChatDto.serializer()) }

    private fun <T> decode(raw: String, element: kotlinx.serialization.KSerializer<T>): List<T>? =
        try {
            json.decodeFromString(ListSerializer(element), raw)
        } catch (_: Exception) {
            null
        }

    // ── Offline action queue ─────────────────────────────────────────────────

    suspend fun pendingActions(): List<PendingAction> =
        readText(KEY_QUEUE)?.let {
            try {
                json.decodeFromString(ListSerializer(PendingAction.serializer()), it)
            } catch (_: Exception) {
                emptyList()
            }
        } ?: emptyList()

    suspend fun enqueueAction(action: PendingAction) {
        val updated = pendingActions() + action
        writeText(KEY_QUEUE, json.encodeToString(ListSerializer(PendingAction.serializer()), updated))
    }

    suspend fun removeAction(id: String) {
        val updated = pendingActions().filterNot { it.id == id }
        writeText(KEY_QUEUE, json.encodeToString(ListSerializer(PendingAction.serializer()), updated))
    }

    /** Wipe everything — used on sign-out. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            dir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
            // best-effort
        }
        Unit
    }

    private companion object {
        const val KEY_PLANS = "plans"
        const val KEY_FRIENDS = "friends"
        const val KEY_CHATS = "chats"
        const val KEY_GROUPS = "groups"
        const val KEY_QUEUE = "pending_actions"
    }
}
