package com.hanghub.app.ui.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

class AppChromeState {
    private val tabBarHiddenCount = mutableIntStateOf(0)

    val isTabBarVisible: Boolean
        get() = tabBarHiddenCount.intValue == 0

    fun pushTabBarHidden(): () -> Unit {
        tabBarHiddenCount.intValue += 1
        var released = false
        return {
            if (!released) {
                released = true
                tabBarHiddenCount.intValue = (tabBarHiddenCount.intValue - 1).coerceAtLeast(0)
            }
        }
    }
}

val LocalAppChrome = staticCompositionLocalOf<AppChromeState> {
    error("LocalAppChrome not provided")
}

@Composable
fun rememberAppChromeState(): AppChromeState = remember { AppChromeState() }
