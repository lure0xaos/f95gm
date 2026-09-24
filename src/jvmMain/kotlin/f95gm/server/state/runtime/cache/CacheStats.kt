package f95gm.server.state.runtime.cache

internal data class CacheStats(val hits: Long, val misses: Long, val evictions: Long) {
    val lookups: Long get() = hits + misses
    val hitRate: Double get() = if (lookups == 0L) 0.0 else hits.toDouble() / lookups
    val missRate: Double get() = if (lookups == 0L) 0.0 else misses.toDouble() / lookups
}
