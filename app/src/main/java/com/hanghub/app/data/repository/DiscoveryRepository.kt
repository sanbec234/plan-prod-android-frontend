package com.hanghub.app.data.repository

import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.core.network.ApiService
import com.hanghub.app.core.network.map
import com.hanghub.app.core.network.safeApiCall
import com.hanghub.app.data.dto.PlaceDto

/**
 * Discovery — nearby ranked places. Wraps GET /api/discovery/places.
 * Mirrors the iOS DiscoveryAPIService.
 */
class DiscoveryRepository(private val api: ApiService) {

    suspend fun getPlaces(
        lat: Double,
        lng: Double,
        vibe: String? = null,
        radius: Int = 2000,
        limit: Int = 30,
    ): ApiResult<List<PlaceDto>> =
        safeApiCall { api.getDiscoveryPlaces(lat, lng, radius, limit, vibe) }.map { it.data }
}
