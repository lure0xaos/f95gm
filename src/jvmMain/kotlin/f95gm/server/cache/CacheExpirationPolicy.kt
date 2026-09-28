package f95gm.server.cache

import f95gm.server.kernel.MonotonicMillis
import f95gm.server.kernel.saturatingAdd
import kotlin.time.Duration

@JvmInline
internal value class CacheTtl(val duration: Duration) {
    init { require(duration.isPositive()) }
}

/** Pure expiry policy; the cache facade remains responsible for synchronization. */
internal class CacheExpirationPolicy(private val ttl: CacheTtl) {
    fun expiresAt(now: MonotonicMillis): MonotonicMillis =
        MonotonicMillis(saturatingAdd(now.value, ttl.duration.inWholeMilliseconds))

    fun isExpired(entry: CacheEntry<*>, now: MonotonicMillis): Boolean = entry.expiresAt <= now
}
