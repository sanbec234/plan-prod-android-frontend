package com.hanghub.app.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <jwt>` to every request when a session token
 * is available. Public endpoints simply ignore the extra header.
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider()
        return if (token.isNullOrBlank()) {
            chain.proceed(request)
        } else {
            chain.proceed(
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            )
        }
    }
}
