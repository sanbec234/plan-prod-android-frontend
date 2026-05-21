package com.hanghub.app.core.network

import com.hanghub.app.core.Config
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Builds the shared [OkHttpClient] (also used for the WebSocket) and the
 * Retrofit-backed [ApiService].
 *
 * @param tokenProvider supplies the current JWT for the Authorization header.
 * @param authenticator optional 401 handler that refreshes the session
 *        (wired in Step 3 once secure token storage exists).
 */
class NetworkModule(
    tokenProvider: () -> String?,
    authenticator: Authenticator? = null,
) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false      // omit null fields on encode (e.g. PATCH profile)
        coerceInputValues = true   // tolerate null for non-null fields with defaults
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenProvider))
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                // BASIC logs only the request line + response status — never headers
                // or bodies, so the Bearer token is not written to logcat.
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)   // keeps the chat WebSocket alive
        .apply { authenticator?.let { authenticator(it) } }
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(Config.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)
}
