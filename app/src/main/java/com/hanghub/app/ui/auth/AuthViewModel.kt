package com.hanghub.app.ui.auth

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanghub.app.core.auth.GoogleAuthClient
import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.data.dto.UserDto
import com.hanghub.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

/** Drives the sign-in screen: Google account picker → backend token exchange. */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleAuth: GoogleAuthClient,
) : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    /** Set once authentication succeeds; observed by the screen to navigate on. */
    var signedInUser by mutableStateOf<UserDto?>(null)
        private set

    val isGoogleConfigured: Boolean get() = googleAuth.isConfigured

    fun beginLoading() {
        uiState = AuthUiState(isLoading = true)
    }

    /** Handle the result of the Google account-picker activity. */
    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            uiState = AuthUiState(isLoading = true)
            val idToken = googleAuth.idTokenFromResult(data)
            if (idToken.isNullOrBlank()) {
                uiState = AuthUiState(
                    error = if (!googleAuth.isConfigured) {
                        "Google Sign-In is not configured. Set default_web_client_id in strings.xml."
                    } else {
                        "Google sign-in was cancelled or failed. Please try again."
                    }
                )
                return@launch
            }
            when (val result = authRepository.signInWithGoogleIdToken(idToken)) {
                is ApiResult.Success -> {
                    uiState = AuthUiState()
                    signedInUser = result.data
                }
                is ApiResult.Failure -> {
                    uiState = AuthUiState(error = result.error.message)
                }
            }
        }
    }
}
