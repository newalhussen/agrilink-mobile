package com.agrilink.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrilink.app.AgriLinkApp
import com.agrilink.app.core.AppError
import com.agrilink.app.di.AppContainer

/** State of a screen that loads something from the server. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>

    /** [refreshing] is true while a pull-to-refresh runs on top of the content already shown. */
    data class Ready<T>(val value: T, val refreshing: Boolean = false) : Load<T>

    data class Failed(val error: AppError) : Load<Nothing>
}

fun <T> Load<T>.valueOrNull(): T? = (this as? Load.Ready<T>)?.value

/** Container of the running app, for building ViewModels. */
@Composable
fun rememberContainer(): AppContainer = (LocalContext.current.applicationContext as AgriLinkApp).container

/** Creates a ViewModel scoped to the current navigation entry, built from the app container. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = rememberContainer()
    val factory = remember(container) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create(container) as T
        }
    }
    return viewModel(key = key, factory = factory)
}
