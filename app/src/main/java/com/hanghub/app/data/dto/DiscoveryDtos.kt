package com.hanghub.app.data.dto

import kotlinx.serialization.Serializable

/** GET /api/discovery/places response. */
@Serializable
data class DiscoveryResponse(val data: List<PlaceDto> = emptyList())

/** A ranked nearby place — mirrors iOS `APIPlace`. */
@Serializable
data class PlaceDto(
    val id: String,
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val category: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val priceLevel: Int? = null,
    val address: String? = null,
    val isOpenNow: Boolean? = null,
    val source: String = "",
    val distanceMetres: Int? = null,
    val vibeMatchLabel: String? = null,
)
