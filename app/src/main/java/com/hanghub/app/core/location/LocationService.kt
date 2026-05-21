package com.hanghub.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper over the fused location provider. Supplies an approximate
 * device location used for discovery queries and reported to the backend.
 * Mirrors the privacy posture of the iOS LocationService (coarse accuracy).
 */
class LocationService(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /** Last known device location as (latitude, longitude), or null. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Pair<Double, Double>? {
        if (!hasPermission()) return null
        return try {
            client.lastLocation.await()?.let { it.latitude to it.longitude }
        } catch (_: Exception) {
            null
        }
    }
}
