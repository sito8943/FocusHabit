package dev.sito8943.focushabit.core.sync

import dev.sito8943.focushabit.core.data.HabitDao
import dev.sito8943.focushabit.core.network.NetworkException
import dev.sito8943.focushabit.core.network.NotConfiguredException
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class UpToDate(val at: Instant) : SyncStatus

    /** [offline] separates "no network" (fixes itself) from a server rejection. */
    data class Failed(val message: String, val offline: Boolean) : SyncStatus
    data object NotConfigured : SyncStatus
}

/**
 * Syncs on its own, same approach as SitoWallet's coordinator: Room re-emits
 * the pending count whenever something changes, so no screen has to remember
 * to ask for a sync. Also syncs when the code changes and when the app comes
 * to the foreground ([refresh]), to pick up changes made on the web.
 *
 * No WorkManager: the only source of changes is the app being open, and what
 * is left pending goes up the next time it opens.
 */
class SyncCoordinator(
    private val engine: SyncEngine,
    private val habits: HabitDao,
    private val code: StateFlow<String>,
    private val isConfigured: Boolean,
    private val scope: CoroutineScope,
    private val now: () -> Instant = Instant::now,
) {

    private val _status = MutableStateFlow<SyncStatus>(
        if (isConfigured) SyncStatus.Idle else SyncStatus.NotConfigured,
    )
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** One sync at a time: two in parallel would push the same rows twice. */
    private val running = Mutex()

    /** Last failure, so a server rejection is not retried in a tight loop. */
    private var lastFailure: Pair<Int, Instant>? = null

    @OptIn(FlowPreview::class)
    fun start() {
        if (!isConfigured) return
        scope.launch {
            combine(habits.observePendingCount(), code) { pending, code -> pending to code }
                .distinctUntilChanged()
                .debounce(DEBOUNCE_MILLIS)
                .collect { (pending, code) ->
                    if (pending > 0 && !inCooldown(pending)) sync(code)
                }
        }
        scope.launch {
            // A new code (restored on this phone) needs its data.
            code.collect { sync(it) }
        }
    }

    /** App back in the foreground, or the sync button. */
    fun refresh() {
        if (!isConfigured) return
        scope.launch { sync(code.value) }
    }

    /**
     * Downloads [newCode] before the caller switches to it, so a failure leaves
     * the current code in place. Pending changes of the current code are pushed
     * first: they keep their own code and are not lost.
     */
    suspend fun restore(newCode: String) {
        if (!isConfigured) throw NotConfiguredException()
        running.withLock {
            runCatching { engine.push() }
            engine.pull(newCode)
        }
    }

    private suspend fun sync(code: String) = running.withLock {
        _status.value = SyncStatus.Syncing
        try {
            engine.push()
            engine.pull(code)
            lastFailure = null
            _status.value = SyncStatus.UpToDate(now())
        } catch (error: CancellationException) {
            throw error
        } catch (error: NetworkException) {
            fail(error, offline = true)
        } catch (error: Exception) {
            fail(error, offline = false)
        }
    }

    private suspend fun fail(error: Exception, offline: Boolean) {
        lastFailure = habits.pendingCount() to now()
        _status.value = SyncStatus.Failed(error.message.orEmpty(), offline)
    }

    /** After a failure, wait before retrying the same thing; new changes retry at once. */
    private fun inCooldown(pending: Int): Boolean {
        val (failedPending, at) = lastFailure ?: return false
        if (pending != failedPending) return false
        return now().toEpochMilli() - at.toEpochMilli() < RETRY_COOLDOWN_MILLIS
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 1_000L
        const val RETRY_COOLDOWN_MILLIS = 60_000L
    }
}
