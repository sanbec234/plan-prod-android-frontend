package com.hanghub.app.data.repository

import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.core.network.ApiService
import com.hanghub.app.core.network.discardValue
import com.hanghub.app.core.network.safeApiCall
import com.hanghub.app.data.dto.LocationRequest
import com.hanghub.app.data.dto.ProfileDto
import com.hanghub.app.data.dto.UpdateProfileRequest

/**
 * Profile + current-user operations. Wraps /api/v1/profile and /users/location.
 * Mirrors the iOS ProfileAPIService.
 */
class ProfileRepository(private val api: ApiService) {

    suspend fun getMyProfile(): ApiResult<ProfileDto> =
        safeApiCall { api.getMyProfile() }

    suspend fun updateProfile(
        displayName: String? = null,
        handle: String? = null,
        avatarId: String? = null,
    ): ApiResult<ProfileDto> =
        safeApiCall { api.updateProfile(UpdateProfileRequest(displayName, handle, avatarId)) }

    /** Report the device's approximate location to the backend. */
    suspend fun updateLocation(latitude: Double, longitude: Double): ApiResult<Unit> =
        safeApiCall { api.updateLocation(LocationRequest(latitude, longitude)) }.discardValue()
}
