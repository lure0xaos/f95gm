package f95gm.server.http.request.ratelimit

import f95gm.server.http.request.RequestEndpoint
import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.EntryCapacity
import f95gm.server.kernel.BoundedAccessMap
import f95gm.server.kernel.MonotonicMillis
import f95gm.server.model.EpochMillis
import f95gm.server.platform.SystemServerClock
import f95gm.server.kernel.elapsedAtLeast
import f95gm.server.kernel.saturatingDouble
import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource
import kotlin.time.Duration

internal class RequestRateLimiter(
    limit: Int,
    windowMillis: Long,
    private val clock: ServerClock = SystemServerClock,
    maxEntries: Int = 4_096
) : LifecycleResource {
    private val closeGuard = CloseGuard()
    internal data class RateLimitNow(val monotonic: MonotonicMillis, val epoch: EpochMillis)
    private val capacity = EntryCapacity(maxEntries)

    internal enum class Outcome { ALLOWED, REJECTED }
    internal data class Decision(val outcome: Outcome, val limit: Int, val remaining: Int, val resetAtMillis: Long) {
        internal constructor(allowed: Boolean, limit: Int, remaining: Int, resetAtMillis: Long) : this(
            if (allowed) Outcome.ALLOWED else Outcome.REJECTED,
            limit,
            remaining,
            resetAtMillis
        )
        val allowed: Boolean get() = outcome == Outcome.ALLOWED
        val resetAt: EpochMillis get() = EpochMillis(resetAtMillis)
    }

    init {
        require(limit > 0) { "limit must be positive" }
        require(windowMillis > 0) { "windowMillis must be positive" }
        require(capacity.value > 0) { "capacity must be positive" }
    }

    private data class BucketKey(val endpoint: RequestEndpoint, val client: String)
    private data class Window(val startedAt: MonotonicMillis, val count: Int)
    private val stateLock = ResourceLock()
    private var policy = LimiterPolicy(limit, windowMillis)
    private val buckets = BucketStore()

    private data class LimiterPolicy(val limit: Int, val windowMillis: Long)

    private inner class BucketStore {
        private val windows = BoundedAccessMap<BucketKey, Window>(capacity)
        private var cleanupCounter = 0L
        private var overflowWindow: Window? = null

        fun record(key: BucketKey, now: RateLimitNow, windowMillis: Long): DecisionData {
            cleanupCounter++
            if (cleanupCounter % 64L == 0L) {
                windows.removeExpired { window ->
                    elapsedAtLeast(now.monotonic.value, window.startedAt.value, saturatingDouble(windowMillis))
                }
                overflowWindow = overflowWindow?.takeUnless { window ->
                    elapsedAtLeast(now.monotonic.value, window.startedAt.value, saturatingDouble(windowMillis))
                }
            }
            val current = windows[key]
            if (current == null && windows.size >= capacity.value) {
                val next = advanceWindow(overflowWindow, now.monotonic, windowMillis)
                overflowWindow = next
                return decisionData(next, now, windowMillis)
            }
            val window = if (current == null || elapsedAtLeast(now.monotonic.value, current.startedAt.value, windowMillis)) {
                Window(now.monotonic, 1)
            } else {
                current.copy(count = current.count.coerceAtMost(Int.MAX_VALUE - 1) + 1)
            }
            windows.put(key, window)
            return decisionData(window, now, windowMillis)
        }

        fun clear() {
            windows.clear()
            overflowWindow = null
        }

        private fun advanceWindow(current: Window?, now: MonotonicMillis, windowMillis: Long): Window =
            if (current == null || elapsedAtLeast(now.value, current.startedAt.value, windowMillis)) {
                Window(now, 1)
            } else {
                current.copy(count = current.count.coerceAtMost(Int.MAX_VALUE - 1) + 1)
            }

        private fun decisionData(window: Window, now: RateLimitNow, windowMillis: Long) = DecisionData(
            count = window.count,
            resetAtMillis = saturatingAdd(
                now.epoch.value,
                (windowMillis - (now.monotonic.value - window.startedAt.value)).coerceAtLeast(0L)
            )
        )
    }

    private data class DecisionData(val count: Int, val resetAtMillis: Long)

    fun updatePolicy(limit: Int, windowMillis: Long) {
        updatePolicy(RequestRateLimit(limit), RateLimitWindow.fromMillis(windowMillis))
    }

    fun updatePolicy(limit: RequestRateLimit, window: RateLimitWindow) {
        stateLock.withLock {
            policy = LimiterPolicy(limit.value, window.value)
        }
    }

    fun tryAcquire(
        key: String,
        now: RateLimitNow? = null,
        endpoint: RequestEndpoint = RequestEndpoint.DEFAULT
    ): Boolean {
        return tryAcquireDetailed(key, now, endpoint).allowed
    }

    fun tryAcquireDetailed(
        key: String,
        now: RateLimitNow? = null,
        endpoint: RequestEndpoint = RequestEndpoint.DEFAULT
    ): Decision = stateLock.withLock {
        check(!closeGuard.isClosed) { "Rate limiter is closed" }
        val currentPolicy = policy
        val limit = currentPolicy.limit
        val windowMillis = currentPolicy.windowMillis
        val typedNow = now ?: RateLimitNow(clock.monotonicMillisTimestamp(), clock.epochMillis())
        val data = buckets.record(BucketKey(endpoint, key), typedNow, windowMillis)
        Decision(
            outcome = if (data.count <= limit) Outcome.ALLOWED else Outcome.REJECTED,
            limit = limit,
            remaining = (limit - data.count).coerceAtLeast(0),
            resetAtMillis = data.resetAtMillis
        )
    }

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right

    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce { stateLock.withLock { buckets.clear() } }
}
