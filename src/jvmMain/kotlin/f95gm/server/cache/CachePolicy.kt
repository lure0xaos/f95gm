package f95gm.server.cache

import kotlin.time.Duration
import f95gm.server.kernel.EntryCapacity

internal data class CachePolicy(
    val capacity: EntryCapacity,
    val ttl: Duration
) {
    init {
        require(ttl.isPositive())
    }

    val maxEntries: Int get() = capacity.value

    companion object {
        fun withCapacity(capacity: EntryCapacity, ttl: Duration): CachePolicy =
            CachePolicy(capacity, ttl)
    }
}
