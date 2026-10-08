package com.agrilink.app.ui.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Runs [block] every time the screen comes back to the foreground (e.g. after closing a detail screen). */
@Composable
fun RefreshOnResume(block: () -> Unit) {
    var first = true
    LifecycleResumeEffect(Unit) {
        if (first) first = false else block()
        onPauseOrDispose { }
    }
}
