package com.hanghub.app.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hanghub.app.data.dto.UserDto
import kotlinx.serialization.json.Json

/**
 * Encrypted persistence for the session JWT and the cached current user.
 * Backed by [EncryptedSharedPreferences] (AES-256) — the Android equivalent of
 * the iOS Keychain. The token is never written in plaintext.
 */
class TokenStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = buildPrefs(context)

    private fun buildPrefs(context: Context) = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        try {
            create(context, masterKey)
        } catch (_: Exception) {
            // Corrupted keyset (rare) — drop the file and recreate cleanly.
            context.deleteSharedPreferences(FILE_NAME)
            create(context, masterKey)
        }
    }

    private fun create(context: Context, masterKey: MasterKey) =
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
            }.apply()
        }

    val isAuthenticated: Boolean get() = !token.isNullOrBlank()

    fun saveUser(user: UserDto) {
        prefs.edit()
            .putString(KEY_USER, json.encodeToString(UserDto.serializer(), user))
            .apply()
    }

    fun readUser(): UserDto? =
        prefs.getString(KEY_USER, null)?.let {
            try {
                json.decodeFromString(UserDto.serializer(), it)
            } catch (_: Exception) {
                null
            }
        }

    /** Wipe the entire session — used on sign-out. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "hh_secure_session"
        const val KEY_TOKEN = "jwt"
        const val KEY_USER = "current_user"
    }
}
