package com.agrilink.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agrilink.app.AgriLinkApp
import com.agrilink.app.core.ApiResult
import com.agrilink.app.ui.common.SystemNotifications

/** Posts a system notification for each new unread AgriLink notification (each only once). */
class NotificationSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as AgriLinkApp).container
        if (container.session.session.value == null) return Result.success()
        return when (val result = container.notifications.list(unreadOnly = true)) {
            is ApiResult.Err -> if (result.error.isNetwork || result.error.httpStatus >= 500) Result.retry() else Result.success()
            is ApiResult.Ok -> {
                val seen = container.settings.notifiedIds()
                val fresh = result.value.items.filter { it.id !in seen }
                fresh.take(MAX_PER_RUN).forEach { SystemNotifications.show(applicationContext, it) }
                container.settings.rememberNotified(fresh.map { it.id })
                Result.success()
            }
        }
    }

    private companion object {
        const val MAX_PER_RUN = 5
    }
}
