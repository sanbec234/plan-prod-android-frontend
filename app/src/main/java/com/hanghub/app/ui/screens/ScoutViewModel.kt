package com.hanghub.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanghub.app.core.location.LocationService
import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.data.HHPlace
import com.hanghub.app.data.repository.DiscoveryRepository
import com.hanghub.app.data.repository.ProfileRepository
import com.hanghub.app.data.toHHPlace
import kotlinx.coroutines.launch

/**
 * Scout tab logic — resolves the device location, reports it to the backend,
 * and loads nearby ranked places from the discovery endpoint.
 */
class ScoutViewModel(
    private val discoveryRepository: DiscoveryRepository,
    private val profileRepository: ProfileRepository,
    private val locationService: LocationService,
) : ViewModel() {

    var places by mutableStateOf<List<HHPlace>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var hasLocationPermission by mutableStateOf(locationService.hasPermission())
        private set

    init {
        if (hasLocationPermission) load()
    }

    fun onPermissionResult(granted: Boolean) {
        hasLocationPermission = granted
        if (granted) load()
    }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            error = null
            val location = locationService.currentLocation()
            if (location == null) {
                error = "Turn on location to discover places nearby."
                isLoading = false
                return@launch
            }
            // Report the device location (fire-and-forget; failure is non-fatal).
            profileRepository.updateLocation(location.first, location.second)
            when (val result = discoveryRepository.getPlaces(location.first, location.second)) {
                is ApiResult.Success -> places = result.data.map { it.toHHPlace() }
                is ApiResult.Failure -> error = result.error.message
            }
            isLoading = false
        }
    }
}
