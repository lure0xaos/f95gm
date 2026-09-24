package f95gm.server.http.request.ratelimit

import f95gm.server.http.request.RequestEndpoint
import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.SystemServerClock
import f95gm.server.state.runtime.elapsedAtLeast
import f95gm.server.state.runtime.saturatingDouble
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class RequestRateLimiter(
    private val limit: Int,
    private val windowMillis: Long,
    private val clock: ServerClock = SystemServerClock,
    private val maxEntries: Int = 4_096
) : AutoCloseable {
    internal data class Decision(val allowed: Boolean, val limit: Int, val remaining: Int, val resetAtMillis: Long)

    init {
        require(limit > 0) { "limit must be positive" }
        require(windowMillis > 0) { "windowMillis must be positive" }
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    private data class BucketKey(val endpoint: RequestEndpoint, val client: String)
    private data class Window(val startedAt: Long, val count: Int)

    private val windows = ConcurrentHashMap<BucketKey, Window>()
    private val cleanupCounter = AtomicLong()

    fun tryAcquire(
        key: String,
        nowMillis: Long = clock.nowMillis(),
        endpoint: RequestEndpoint = RequestEndpoint.DEFAULT
    ): Boolean {
        return tryAcquireDetailed(key, nowMillis, endpoint).allowed
    }

    fun tryAcquireDetailed(
        key: String,
        nowMillis: Long = clock.nowMillis(),
        endpoint: RequestEndpoint = RequestEndpoint.DEFAULT
    ): Decision {
        val bucketKey = BucketKey(endpoint, key)
        if (cleanupCounter.incrementAndGet() % 64L == 0L) {
            windows.entries.removeIf { (_, window) ->
                elapsedAtLeast(
                    nowMillis,
                    window.startedAt,
                    saturatingDouble(windowMillis)
                )
            }
        }
        val window = windows.compute(bucketKey) { _, current ->
            if (current == null || elapsedAtLeast(nowMillis, current.startedAt, windowMillis)) Window(nowMillis, 1)
            else current.copy(count = current.count.coerceAtMost(Int.MAX_VALUE - 1) + 1)
        } ?: return Decision(false, limit, 0, nowMillis)
        if (windows.size > maxEntries) {
            windows.entries.minByOrNull { it.value.startedAt }?.let { windows.remove(it.key, it.value) }
        }
        return Decision(
            allowed = window.count <= limit,
            limit = limit,
            remaining = (limit - window.count).coerceAtLeast(0),
            resetAtMillis = saturatingAdd(window.startedAt, windowMillis)
        )
    }

    fun tryAcquire(key: String, nowMillis: Long, endpoint: String): Boolean =
        tryAcquire(key, nowMillis, RequestEndpoint(endpoint))

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right

    override fun close() = windows.clear()
}
