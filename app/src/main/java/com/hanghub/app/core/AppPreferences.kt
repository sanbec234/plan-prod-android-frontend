package com.hanghub.app.core

import android.content.Context

/**
 * Non-sensitive app preferences (theme choice, etc.) — plain SharedPreferences.
 * Session secrets live in the encrypted TokenStore, never here.
 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("hh_prefs", Context.MODE_PRIVATE)

    /** "system" | "light" | "dark" */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) {
            prefs.edit().putString(KEY_THEME, value).apply()
        }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        private const val KEY_THEME = "theme_mode"
    }
}
