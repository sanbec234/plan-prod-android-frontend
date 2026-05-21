package com.hanghub.app

import android.app.Application
import com.hanghub.app.core.AppContainer

/**
 * Process-wide entry point. Owns the [AppContainer] service locator that holds
 * the networking, auth, and repository singletons (manual DI — no Hilt).
 */
class PlanApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
