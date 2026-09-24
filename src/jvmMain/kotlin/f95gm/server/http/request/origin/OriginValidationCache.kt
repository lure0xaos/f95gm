package f95gm.server.http.request.origin

import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.SystemServerClock
import f95gm.server.state.runtime.cache.BoundedTtlCache
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class OriginValidationCache(
    maxEntries: Int = 256,
    ttl: Duration = 5.minutes,
    private val clock: ServerClock = SystemServerClock
) : AutoCloseable {
    private val values = BoundedTtlCache.withTtl<String, Boolean>(maxEntries, ttl, clock)

    fun getOrPut(key: String, nowMillis: Long = clock.nowMillis(), compute: () -> Boolean): Boolean =
        values.getOrPutAt(key, nowMillis, compute)

    override fun close() = values.clear()
}
