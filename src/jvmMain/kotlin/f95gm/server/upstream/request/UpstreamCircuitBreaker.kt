package f95gm.server.upstream.request

import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.elapsedAtLeast
import f95gm.server.state.runtime.saturatingDouble
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class UpstreamCircuitBreaker(
    private val clock: ServerClock,
    private val failureThreshold: Int = 5,
    private val openDuration: Duration = 30_000.milliseconds,
    private val maxEntries: Int = 4_096,
    private val onOpened: () -> Unit = {}
) : AutoCloseable {
    private data class State(val failures: Int = 0, val openedAt: Long? = null, val lastTouchedAt: Long = 0L)

    private val states = ConcurrentHashMap<String, State>()
    private val cleanupCounter = AtomicLong()

    init {
        require(failureThreshold > 0)
        require(openDuration.isPositive())
        require(maxEntries > 0)
    }

    fun allow(key: String): Boolean {
        val now = clock.nowMillis()
        if (cleanupCounter.incrementAndGet() % 64L == 0L) {
            states.entries.removeIf {
                elapsedAtLeast(
                    now,
                    it.value.lastTouchedAt,
                    saturatingDouble(openDuration.inWholeMilliseconds)
                )
            }
        }
        val current = states.compute(key) { _, state ->
            val current = state ?: State(lastTouchedAt = now)
            val openedAt = current.openedAt
            if (openedAt != null && elapsedAtLeast(
                    now,
                    openedAt,
                    openDuration.inWholeMilliseconds
                )
            ) State(lastTouchedAt = now)
            else current.copy(lastTouchedAt = now)
        } ?: return true
        while (states.size > maxEntries) {
            states.entries.minByOrNull { it.value.lastTouchedAt }?.let { states.remove(it.key, it.value) } ?: break
        }
        return current.openedAt == null
    }

    fun recordSuccess(key: String) {
        states.remove(key)
    }

    fun recordFailure(key: String) {
        states.compute(key) { _, state ->
            val now = clock.nowMillis()
            val current = state ?: State(lastTouchedAt = now)
            val failures = current.failures + 1
            val openedAt = if (failures >= failureThreshold && current.openedAt == null) {
                onOpened()
                now
            } else current.openedAt
            State(failures, openedAt, now)
        }
    }

    override fun close() = states.clear()
}
