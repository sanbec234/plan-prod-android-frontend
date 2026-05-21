package com.hanghub.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanghub.app.core.network.ApiError
import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.data.repository.ProfileRepository
import com.hanghub.app.ui.state.AppStateViewModel
import kotlinx.coroutines.launch

/** Profile editing — display name and @handle updates. */
class ProfileViewModel(
    private val appState: AppStateViewModel,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    var isSaving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    fun updateProfile(displayName: String, handle: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isSaving = true
            saveError = null
            val result = profileRepository.updateProfile(
                displayName = displayName.trim().ifEmpty { null },
                handle = handle.trim().ifEmpty { null },
            )
            isSaving = false
            when (result) {
                is ApiResult.Success -> {
                    appState.updateCurrentUserProfile(result.data.displayName, result.data.handle)
                    onResult(true)
                }
                is ApiResult.Failure -> {
                    if (result.error is ApiError.Unauthorized) appState.onUnauthorized()
                    saveError = result.error.message
                    onResult(false)
                }
            }
        }
    }
}
