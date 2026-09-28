package f95gm.server.upstream.request

import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.MonotonicNanos
import f95gm.server.kernel.EntryCapacity
import f95gm.server.kernel.BoundedAccessMap
import f95gm.server.kernel.elapsedAtLeast
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource

internal class UpstreamCircuitBreaker(
    private val clock: ServerClock,
    private val failureThreshold: Int = 5,
    private val openDuration: Duration = 30_000.milliseconds,
    maxEntries: Int = 4_096,
    private val onOpened: () -> Unit = {}
) : LifecycleResource {
    private val closeGuard = CloseGuard()
    private val capacity = EntryCapacity(maxEntries)
    private sealed interface State {
        val lastTouchedAt: MonotonicNanos
        data class Closed(val failures: Int, override val lastTouchedAt: MonotonicNanos) : State
        data class Open(val openedAt: MonotonicNanos, override val lastTouchedAt: MonotonicNanos) : State
    }

    private val states = BoundedAccessMap<UpstreamCircuitKey, State>(capacity)
    private val stateLock = ResourceLock()
    private var cleanupCounter = 0L

    val size: Int get() = stateLock.withLock { states.size }

    init {
        require(failureThreshold > 0)
        require(openDuration.isPositive())
    }

    fun allow(key: UpstreamCircuitKey): Boolean = stateLock.withLock {
        val now = clock.monotonicTimestamp()
        cleanupCounter += 1L
        if (cleanupCounter % 64L == 0L) {
            states.removeIf { _, state ->
                elapsedAtLeast(now, state.lastTouchedAt, openDuration * 2)
            }
        }
        val current = states[key] ?: State.Closed(0, now)
        val next = when (current) {
            is State.Open -> if (elapsedAtLeast(now, current.openedAt, openDuration)) {
                State.Closed(0, now)
            } else {
                current.copy(lastTouchedAt = now)
            }
            is State.Closed -> current.copy(lastTouchedAt = now)
        }
        states.put(key, next)
        next is State.Closed
    }

    fun recordSuccess(key: UpstreamCircuitKey) {
        stateLock.withLock { states.remove(key) }
    }

    fun recordFailure(key: UpstreamCircuitKey) {
        var opened = false
        stateLock.withLock {
            val now = clock.monotonicTimestamp()
            val current = states[key] ?: State.Closed(0, now)
            when (current) {
                is State.Open -> states.put(key, current.copy(lastTouchedAt = now))
                is State.Closed -> {
                    val failures = current.failures + 1
                    if (failures >= failureThreshold) {
                        opened = true
                        states.put(key, State.Open(now, now))
                    } else {
                        states.put(key, State.Closed(failures, now))
                    }
                }
            }
        }
        if (opened) onOpened()
    }

    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce { stateLock.withLock { states.clear() } }
}
