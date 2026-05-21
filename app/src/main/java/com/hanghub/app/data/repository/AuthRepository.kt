package com.hanghub.app.data.repository

import com.hanghub.app.core.auth.GoogleAuthClient
import com.hanghub.app.core.auth.JwtUtils
import com.hanghub.app.core.auth.TokenStore
import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.core.network.ApiService
import com.hanghub.app.core.network.safeApiCall
import com.hanghub.app.data.dto.GoogleAuthRequest
import com.hanghub.app.data.dto.UserDto

/**
 * Owns the session lifecycle: Google id_token → backend exchange → encrypted
 * persistence, plus proactive refresh of the 90-day backend JWT.
 */
class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
    private val googleAuth: GoogleAuthClient,
) {

    val isAuthenticated: Boolean get() = tokenStore.isAuthenticated
    val currentUser: UserDto? get() = tokenStore.readUser()
    val isGoogleConfigured: Boolean get() = googleAuth.isConfigured

    /** True when the stored JWT is within the refresh window or already expired. */
    val needsRefresh: Boolean get() = JwtUtils.isExpiringSoon(tokenStore.token)

    /** True when the stored JWT has already expired. */
    val isSessionExpired: Boolean get() = JwtUtils.isExpired(tokenStore.token)

    /** Exchange a Google id_token for a backend session and persist it. */
    suspend fun signInWithGoogleIdToken(idToken: String): ApiResult<UserDto> =
        when (val result = safeApiCall { api.googleAuth(GoogleAuthRequest(idToken = idToken)) }) {
            is ApiResult.Success -> {
                tokenStore.token = result.data.token
                tokenStore.saveUser(result.data.user)
                ApiResult.Success(result.data.user)
            }
            is ApiResult.Failure -> result
        }

    /**
     * Silently refresh the backend session using a cached Google account.
     * Returns true on success. Called proactively (token near expiry) and
     * after a 401.
     */
    suspend fun refreshSession(): Boolean {
        val idToken = googleAuth.silentIdToken() ?: return false
        return signInWithGoogleIdToken(idToken) is ApiResult.Success
    }

    suspend fun signOut() {
        googleAuth.signOut()
        tokenStore.clear()
    }
}
