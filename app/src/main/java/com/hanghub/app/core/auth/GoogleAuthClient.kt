@file:Suppress("DEPRECATION")

package com.hanghub.app.core.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.hanghub.app.R
import kotlinx.coroutines.tasks.await

/**
 * Wraps Google Sign-In. Produces an OAuth `id_token` that is exchanged with the
 * backend `POST /auth/google`, mirroring the iOS `AuthService`.
 *
 * The classic GoogleSignIn API is used (one dependency, returns an id_token
 * directly); its deprecation is suppressed at the file level.
 */
class GoogleAuthClient(private val context: Context) {

    private companion object {
        const val TAG = "GoogleAuthClient"
    }

    /** The backend's OAuth *web* client ID — see strings.xml `default_web_client_id`. */
    private val webClientId: String = context.getString(R.string.default_web_client_id)

    /** False when `default_web_client_id` has not been configured. */
    val isConfigured: Boolean get() = webClientId.isNotBlank()

    private val client: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .apply { if (webClientId.isNotBlank()) requestIdToken(webClientId) }
            .build()
        GoogleSignIn.getClient(context, options)
    }

    /** Intent to launch the interactive Google account picker. */
    fun signInIntent(): Intent = client.signInIntent

    /** Extract a fresh id_token from an interactive sign-in result. */
    suspend fun idTokenFromResult(data: Intent?): String? =
        try {
            GoogleSignIn.getSignedInAccountFromIntent(data).await().idToken
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in ApiException: statusCode=${e.statusCode} message=${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            null
        }

    /**
     * Silent re-auth — returns a fresh id_token when a Google account is
     * already cached on the device. Used to refresh the backend session.
     */
    suspend fun silentIdToken(): String? =
        try {
            client.silentSignIn().await()?.idToken
        } catch (e: ApiException) {
            Log.e(TAG, "Silent sign-in ApiException: statusCode=${e.statusCode}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Silent sign-in failed", e)
            null
        }

    suspend fun signOut() {
        try {
            client.signOut().await()
        } catch (_: Exception) {
            // Sign-out is best-effort; the local session is cleared regardless.
        }
    }
}
