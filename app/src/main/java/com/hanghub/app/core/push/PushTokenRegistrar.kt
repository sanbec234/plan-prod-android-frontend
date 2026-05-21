package com.hanghub.app.core.push

import com.google.firebase.messaging.FirebaseMessaging
import com.hanghub.app.core.network.ApiService
import com.hanghub.app.core.network.safeApiCall
import com.hanghub.app.data.dto.DeviceTokenRequest
import kotlinx.coroutines.tasks.await

/**
 * Registers an FCM device token with `POST /notifications/device-token`.
 *
 * NOTE: the backend currently delivers push only via APNs (iOS). This path is
 * forward-compatible — it records an Android token so delivery works once the
 * backend adds FCM. If Firebase is not configured (no `google-services.json`),
 * token retrieval fails gracefully and this is a no-op.
 */
class PushTokenRegistrar(private val api: ApiService) {

    suspend fun register() {
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (_: Throwable) {
            // Firebase not configured — expected until google-services.json is added.
            null
        }
        if (token.isNullOrBlank()) return
        // Result intentionally ignored — registration is best-effort.
        safeApiCall {
            api.registerDeviceToken(DeviceTokenRequest(token = token, platform = "android"))
        }
    }
}
