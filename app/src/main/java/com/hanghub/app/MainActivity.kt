package com.hanghub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
	import androidx.compose.runtime.LaunchedEffect
	import androidx.compose.runtime.getValue
	import androidx.compose.runtime.mutableStateOf
	import androidx.compose.runtime.remember
	import androidx.compose.runtime.rememberCoroutineScope
	import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanghub.app.core.appContainer
import com.hanghub.app.core.push.DeepLinkHandler
import com.hanghub.app.core.push.DeepLinkTarget
import com.hanghub.app.core.viewModelFactory
import com.hanghub.app.ui.auth.AuthScreen
import com.hanghub.app.ui.state.AppStateViewModel
import com.hanghub.app.ui.state.LocalAppState
import com.hanghub.app.ui.screens.CirclesScreen
import com.hanghub.app.ui.screens.CreatePlanFlow
import com.hanghub.app.ui.screens.PlansScreen
	import com.hanghub.app.ui.screens.ProfileScreen
	import com.hanghub.app.ui.screens.ScoutScreen
	import com.hanghub.app.ui.chrome.LocalAppChrome
	import com.hanghub.app.ui.chrome.rememberAppChromeState
	import com.hanghub.app.ui.theme.HHMotion
	import com.hanghub.app.ui.theme.HHType
	import com.hanghub.app.ui.theme.HangHubTheme
	import com.hanghub.app.ui.theme.hh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Activity
// ═══════════════════════════════════════════════════════════════════════════

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DeepLinkHandler.handleIntent(intent)
        setContent {
            val container = appContainer()
            val appState: AppStateViewModel = viewModel(
                factory = viewModelFactory {
                    AppStateViewModel(
                        container.authRepository,
                        container.planRepository,
                        container.friendsRepository,
                        container.chatRepository,
                        container.webSocketManager,
                        container.appPreferences,
                        container.localCache,
                        container.networkMonitor,
                    )
                }
            )
            // Register the FCM token once authenticated (best-effort).
            LaunchedEffect(appState.isAuthenticated) {
                if (appState.isAuthenticated) container.pushTokenRegistrar.register()
            }
            val darkTheme = when (appState.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            HangHubTheme(darkTheme = darkTheme) {
                // Auth gate — sign-in screen until a session exists.
                if (appState.isAuthenticated) {
                    HangHubApp(appState)
                } else {
                    AuthScreen(
                        container = container,
                        onAuthenticated = appState::onSignedIn,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinkHandler.handleIntent(intent)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: App Shell
// ═══════════════════════════════════════════════════════════════════════════

enum class AppTab(val label: String, @DrawableRes val iconRes: Int, val route: String) {
    SCOUT("Scout",     R.drawable.ic_nav_discovery, "scout"),
    CIRCLES("Circles", R.drawable.ic_nav_users, "circles"),
    PLANS("Hangouts",  R.drawable.ic_nav_calendar, "plans"),
    PROFILE("Profile", R.drawable.ic_nav_profile, "profile"),
}

data class ToastData(val text: String, val sub: String? = null, val id: Long = System.currentTimeMillis())

@Composable
	fun HangHubApp(appState: AppStateViewModel) {
	    val c = hh
	    var activeTab by remember { mutableStateOf(AppTab.SCOUT) }
	    var showCreatePlan by remember { mutableStateOf(false) }
	    var toast by remember { mutableStateOf<ToastData?>(null) }
	    val scope = rememberCoroutineScope()
	    val chrome = rememberAppChromeState()

    fun showToast(data: ToastData) {
        toast = data
        scope.launch {
            delay(2200)
            toast = null
        }
    }

    // Refresh when the app returns to the foreground (Android "background refresh"
    // equivalent — WorkManager was intentionally not added to the dependency set).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        appState.loadInitialData()
    }

    // Route notification taps (deep links) to the matching tab.
    LaunchedEffect(DeepLinkHandler.pending) {
        when (DeepLinkHandler.consume()) {
            is DeepLinkTarget.Chat -> activeTab = AppTab.CIRCLES
            is DeepLinkTarget.Plan -> activeTab = AppTab.PLANS
            null -> Unit
        }
    }

	    CompositionLocalProvider(LocalAppChrome provides chrome, LocalAppState provides appState) {
	        // Hide tab bar while CreatePlanFlow is shown
	        DisposableEffect(showCreatePlan) {
	            val pop = if (showCreatePlan) chrome.pushTabBarHidden() else null
	            onDispose { pop?.invoke() }
	        }

	        Box(
	            modifier = Modifier
	                .fillMaxSize()
	                .background(c.bg)
	                .windowInsetsPadding(WindowInsets.statusBars)
	        ) {
	        // ── Screen content ─────────────────────────────────────────────
	        when (activeTab) {
	            AppTab.SCOUT   -> ScoutScreen(
	                onCreatePlan = { showCreatePlan = true },
	                showToast = { showToast(it) }
            )
            AppTab.CIRCLES -> CirclesScreen(showToast = { showToast(it) })
            AppTab.PLANS   -> PlansScreen(
                onCreatePlan = { showCreatePlan = true },
                showToast = { showToast(it) }
            )
            AppTab.PROFILE -> ProfileScreen()
        }

	        // ── Floating Tab Bar ───────────────────────────────────────────
	        if (chrome.isTabBarVisible) {
	            Box(
	                modifier = Modifier
	                    .align(Alignment.BottomCenter)
	                    .padding(horizontal = 12.dp)
	                    .padding(bottom = 12.dp)
	                    .windowInsetsPadding(WindowInsets.navigationBars)
	            ) {
	                FloatingTabBar(activeTab = activeTab, onTabSelected = { activeTab = it })
	            }
	        }

        // ── Toast ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = toast != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp),
            enter = slideInVertically { -it } + fadeIn(),
            exit  = slideOutVertically { -it } + fadeOut(),
        ) {
            toast?.let { ToastView(it) }
        }

        // ── Offline banner ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = appState.isOffline,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            enter = slideInVertically { -it } + fadeIn(),
            exit  = slideOutVertically { -it } + fadeOut(),
        ) {
            OfflineBanner()
        }

	        // ── Create Plan Sheet ──────────────────────────────────────────
	        if (showCreatePlan) {
	            CreatePlanFlow(
	                onDismiss = { showCreatePlan = false },
	                onCreated = {
                    showCreatePlan = false
                    showToast(ToastData("Plan sent!", "Invites out to 2 friends"))
                }
	            )
	        }
	        }
	    }
	}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: FloatingTabBar
// Frosted pill — matches prototype TabBar exactly
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun FloatingTabBar(activeTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    val c = hh
    Surface(
        shape = CircleShape,
        color = c.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, c.stroke),
        shadowElevation = 16.dp,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AppTab.entries.forEach { tab ->
                TabPill(
                    tab = tab,
                    isActive = activeTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
fun TabPill(tab: AppTab, isActive: Boolean, onClick: () -> Unit) {
    val c = hh
    val bgColor by animateColorAsState(
        targetValue = if (isActive) c.accent else Color.Transparent,
        animationSpec = HHMotion.subtle(),
        label = "tabBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isActive) c.onAccent else c.inkMute,
        animationSpec = HHMotion.subtle(),
        label = "tabIcon"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = if (isActive) 16.dp else 0.dp)
                .then(if (!isActive) Modifier.width(48.dp) else Modifier)
        ) {
            Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = tab.label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            AnimatedVisibility(
                visible = isActive,
                enter = expandHorizontally() + fadeIn(),
                exit  = shrinkHorizontally() + fadeOut(),
            ) {
                Row {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tab.label,
                        style = HHType.caption,
                        fontWeight = FontWeight.Bold,
                        color = c.onAccent,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: ToastView
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun OfflineBanner() {
    val c = hh
    Surface(
        shape = RoundedCornerShape(com.hanghub.app.ui.theme.HHRadius.lg),
        color = c.ink,
        shadowElevation = 8.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text("⚡", fontSize = 13.sp)
            Text("You're offline — showing cached data", style = HHType.caption, color = c.bg)
        }
    }
}

@Composable
fun ToastView(data: ToastData) {
    val c = hh
    Surface(
        shape = RoundedCornerShape(com.hanghub.app.ui.theme.HHRadius.lg),
        color = c.surface,
        border = BorderStroke(1.dp, c.stroke),
        shadowElevation = 16.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(c.accentSoft)
            ) { Text("🎉", fontSize = 18.sp) }
            Column {
                Text(data.text, style = HHType.bodyMd, color = c.ink)
                data.sub?.let { Text(it, style = HHType.caption, color = c.inkMute) }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}
