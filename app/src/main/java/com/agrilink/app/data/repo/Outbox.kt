package com.agrilink.app.data.repo

import com.agrilink.app.data.local.PendingActionDao
import com.agrilink.app.data.local.PendingActionEntity
import kotlinx.coroutines.flow.Flow

/** What happened to an action the user took. */
sealed interface ActionResult<out T> {
    data class Done<T>(val value: T) : ActionResult<T>

    /** No signal: saved on the phone and will be sent automatically when the network returns. */
    data object Queued : ActionResult<Nothing>

    data class Failed(val error: com.agrilink.app.core.AppError) : ActionResult<Nothing>
}

enum class ReplayOutcome { DONE, RETRY, DROP }

/**
 * Offline queue for actions that can safely wait (accept an order, mark it ready, reject it).
 * Handover steps that depend on a live code check are deliberately not queued.
 */
class Outbox(
    private val dao: PendingActionDao,
    private val schedule: () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val pendingCount: Flow<Int> = dao.count()

    suspend fun enqueue(kind: String, method: String, path: String, body: String?, label: String) {
        dao.insert(PendingActionEntity(kind = kind, method = method, path = path, body = body, label = label, createdAt = now()))
        schedule()
    }

    /**
     * Replays the queue in order. [execute] returns the outcome for one action; a RETRY stops the run so ordering is
     * preserved and WorkManager can try again later. Actions are dropped after [MAX_ATTEMPTS] failed attempts.
     */
    suspend fun flush(execute: suspend (PendingActionEntity) -> ReplayOutcome): FlushResult {
        var sent = 0
        var dropped = 0
        for (action in dao.all()) {
            when (execute(action)) {
                ReplayOutcome.DONE -> { dao.delete(action.id); sent++ }
                ReplayOutcome.DROP -> { dao.delete(action.id); dropped++ }
                ReplayOutcome.RETRY -> {
                    dao.markAttempt(action.id)
                    if (action.attempts + 1 >= MAX_ATTEMPTS) { dao.delete(action.id); dropped++ } else return FlushResult(sent, dropped, remaining = true)
                }
            }
        }
        return FlushResult(sent, dropped, remaining = false)
    }

    suspend fun clear() = dao.clear()

    data class FlushResult(val sent: Int, val dropped: Int, val remaining: Boolean)

    companion object {
        const val MAX_ATTEMPTS = 8

        /** Maps an HTTP status to what the outbox should do with the action. */
        fun outcomeFor(status: Int?): ReplayOutcome = when {
            status == null -> ReplayOutcome.RETRY
            status in 200..299 -> ReplayOutcome.DONE
            status == 401 || status == 408 || status == 429 || status >= 500 -> ReplayOutcome.RETRY
            // 400, 403, 404, 409 ...: the situation changed (order expired, already answered); replaying cannot help.
            else -> ReplayOutcome.DROP
        }
    }
}
