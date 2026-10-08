package com.fincahernandez.gestionpecuaria.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class AutoSyncPhase { WAITING, OFFLINE, RUNNING, COMPLETE, PENDING, REVIEW, RETRY }
data class AutoSyncStatus(
    val phase: AutoSyncPhase,
    val completedAt: Long? = null,
    val detail: String? = null
)

/** One foreground loop. Coalesces edits, serializes requests, and backs off on errors. */
class AutoSyncLoop(
    private val sync: suspend () -> SyncResult,
    private val report: (AutoSyncStatus) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
    private val settleMs: Long = 2_000,
    private val pollMs: Long = 60_000,
    private val retryMs: Long = 15_000,
    private val maxRetryMs: Long = 300_000
) {
    suspend fun run(changes: Flow<Unit>): Unit = coroutineScope {
        val signals = Channel<Unit>(Channel.CONFLATED)
        val observer = launch { changes.collect { signals.trySend(Unit) } }
        var waitMs = 0L
        var failureDelay = retryMs
        var mustBackOff = false
        try {
            while (isActive) {
                if (mustBackOff) delay(waitMs)
                else if (waitMs > 0) withTimeoutOrNull(waitMs) { signals.receive() }
                delay(settleMs)
                // The forthcoming snapshot includes edits queued before this point.
                signals.tryReceive()
                report(AutoSyncStatus(AutoSyncPhase.RUNNING))
                try {
                    val result = sync()
                    mustBackOff = false
                    failureDelay = retryMs
                    when {
                        result.conflicts.isNotEmpty() -> {
                            report(AutoSyncStatus(AutoSyncPhase.REVIEW))
                            waitMs = maxRetryMs
                        }
                        result.pending > 0 -> {
                            report(AutoSyncStatus(AutoSyncPhase.PENDING))
                            waitMs = settleMs
                        }
                        else -> {
                            report(AutoSyncStatus(AutoSyncPhase.COMPLETE, now()))
                            waitMs = pollMs
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val review = e is SyncRejected || e is IllegalArgumentException || e is CloudAccessDenied
                    report(
                        AutoSyncStatus(
                            phase = if (review) AutoSyncPhase.REVIEW else AutoSyncPhase.RETRY,
                            detail = e.message?.trim()?.takeIf(String::isNotBlank)
                        )
                    )
                    waitMs = if (review) maxRetryMs else failureDelay
                    failureDelay = (failureDelay * 2).coerceAtMost(maxRetryMs)
                    mustBackOff = true
                }
            }
        } finally {
            observer.cancel()
            signals.close()
        }
    }
}
