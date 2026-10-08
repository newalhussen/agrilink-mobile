package com.agrilink.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agrilink.app.AgriLinkApp
import com.agrilink.app.data.repo.Outbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sends the actions the user took while offline, oldest first, stopping at the first one that needs a retry. */
class OutboxWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as AgriLinkApp).container
        if (container.session.session.value == null) {
            container.outbox.clear()
            return Result.success()
        }
        val result = container.outbox.flush { action ->
            val status = withContext(Dispatchers.IO) { container.apiFactory.replay(action.method, action.path, action.body) }
            Outbox.outcomeFor(status)
        }
        return if (result.remaining) Result.retry() else Result.success()
    }
}
