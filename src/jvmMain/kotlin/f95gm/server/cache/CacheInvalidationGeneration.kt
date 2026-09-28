package f95gm.server.cache

import java.util.concurrent.atomic.AtomicLong

/** Coordinates stale-loader suppression independently from cache storage. */
@JvmInline
internal value class CacheGeneration(val value: Long) {
    init { require(value >= 0L) }
}

internal class CacheInvalidationGeneration {
    private val value = AtomicLong()
    fun current(): CacheGeneration = CacheGeneration(value.get())
    fun advance(): CacheGeneration = CacheGeneration(value.incrementAndGet())
}
