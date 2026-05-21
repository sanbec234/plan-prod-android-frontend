package com.hanghub.app.core

import android.content.Context
import com.hanghub.app.core.auth.GoogleAuthClient
import com.hanghub.app.core.auth.TokenStore
import com.hanghub.app.core.network.ApiService
import com.hanghub.app.core.location.LocationService
import com.hanghub.app.core.network.NetworkModule
import com.hanghub.app.core.network.NetworkMonitor
import com.hanghub.app.core.storage.LocalCache
import com.hanghub.app.core.network.WebSocketManager
import com.hanghub.app.core.push.PushTokenRegistrar
import com.hanghub.app.data.repository.AuthRepository
import com.hanghub.app.data.repository.ChatRepository
import com.hanghub.app.data.repository.DiscoveryRepository
import com.hanghub.app.data.repository.FriendsRepository
import com.hanghub.app.data.repository.PlanRepository
import com.hanghub.app.data.repository.ProfileRepository

/**
 * Manual dependency-injection container — the Android equivalent of the iOS
 * `*.shared` singletons. A single instance lives on [com.hanghub.app.PlanApplication]
 * and is reached from Composables via [appContainer].
 *
 * Feature repositories are added in later steps.
 */
class AppContainer(val appContext: Context) {

    // ── Auth + secure storage ───────────────────────────────────────────────
    val tokenStore: TokenStore by lazy { TokenStore(appContext) }

    val googleAuth: GoogleAuthClient by lazy { GoogleAuthClient(appContext) }

    // ── Networking ──────────────────────────────────────────────────────────
    private val network: NetworkModule by lazy {
        NetworkModule(tokenProvider = { tokenStore.token })
    }

    val apiService: ApiService get() = network.apiService

    val webSocketManager: WebSocketManager by lazy {
        WebSocketManager(network.okHttpClient, tokenProvider = { tokenStore.token })
    }

    // ── Repositories ────────────────────────────────────────────────────────
    val authRepository: AuthRepository by lazy {
        AuthRepository(apiService, tokenStore, googleAuth)
    }

    val planRepository: PlanRepository by lazy { PlanRepository(apiService) }

    val friendsRepository: FriendsRepository by lazy { FriendsRepository(apiService) }

    val chatRepository: ChatRepository by lazy { ChatRepository(apiService) }

    val discoveryRepository: DiscoveryRepository by lazy { DiscoveryRepository(apiService) }

    val profileRepository: ProfileRepository by lazy { ProfileRepository(apiService) }

    val locationService: LocationService by lazy { LocationService(appContext) }

    val appPreferences: AppPreferences by lazy { AppPreferences(appContext) }

    val localCache: LocalCache by lazy { LocalCache(appContext) }

    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(appContext) }

    val pushTokenRegistrar: PushTokenRegistrar by lazy { PushTokenRegistrar(apiService) }
}
