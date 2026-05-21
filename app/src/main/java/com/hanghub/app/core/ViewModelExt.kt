package com.hanghub.app.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hanghub.app.PlanApplication

/**
 * Builds a [ViewModelProvider.Factory] from a plain initializer lambda so
 * screens can construct ViewModels with constructor-injected repositories.
 */
@Suppress("UNCHECKED_CAST")
inline fun <VM : ViewModel> viewModelFactory(crossinline initializer: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = initializer() as T
    }

/** Reaches the process-wide [AppContainer] from within a Composable. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as PlanApplication).container
