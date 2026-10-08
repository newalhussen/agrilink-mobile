package com.agrilink.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Background jobs: replay the offline outbox when signal returns, and pick up new notifications periodically. */
class WorkScheduler(private val context: Context) {

    private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun enqueueOutbox() {
        val request = OneTimeWorkRequestBuilder<OutboxWorker>()
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(OUTBOX, ExistingWorkPolicy.REPLACE, request)
    }

    /** Without Firebase, this is how updates reach a phone whose app is closed (Android's minimum interval is 15 min). */
    fun schedulePeriodicNotificationSync() {
        val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connected)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOTIFICATIONS, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelAll() {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(OUTBOX)
        wm.cancelUniqueWork(NOTIFICATIONS)
    }

    private companion object {
        const val OUTBOX = "agrilink-outbox"
        const val NOTIFICATIONS = "agrilink-notification-sync"
    }
}
