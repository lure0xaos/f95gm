package f95gm.server.http.static

import f95gm.server.kernel.BoundedAccessMap
import f95gm.server.kernel.EntryCapacity
import f95gm.server.kernel.ResourceLock
import f95gm.server.cache.CacheNamespaceStats
import f95gm.server.cache.CacheNamespaces
import f95gm.server.cache.CacheStatisticsProvider
import f95gm.server.cache.CacheStatisticsSnapshot
import f95gm.server.cache.CacheStats
import java.nio.file.Path

private data class CachedOverride(val size: Long, val modifiedAt: java.nio.file.attribute.FileTime, val resource: StaticResource)

internal class OverrideResourceCache(private val capacity: EntryCapacity = EntryCapacity(256)) : CacheStatisticsProvider {
    private val values = BoundedAccessMap<Path, CachedOverride>(capacity)
    private val lock = ResourceLock()
    private var hits = 0L
    private var misses = 0L
    private var evictions = 0L

    fun getOrLoad(file: StaticFileAccess): StaticResource = lock.withLock {
        val existing = values[file.path]
        existing?.takeIf { it.size == file.size && it.modifiedAt == file.modifiedAt }?.let {
            hits++
            return@withLock it.resource
        }
        misses++
        val willEvict = existing == null && values.size >= capacity.value
        val resource = file.readBytes().let { bytes -> StaticResource(bytes, etag(bytes)) }
        values.put(file.path, CachedOverride(file.size, file.modifiedAt, resource))
        if (willEvict) evictions++
        resource
    }

    fun clear() = lock.withLock { values.clear() }

    override fun snapshot(): CacheStatisticsSnapshot = lock.withLock {
        val statistics = CacheStats(hits = hits, misses = misses, evictions = evictions)
        CacheStatisticsSnapshot(
            namespaces = mapOf(
                CacheNamespaces.STATIC to CacheNamespaceStats(
                    entries = values.size.toLong(),
                    hits = hits,
                    misses = misses,
                    evictions = evictions,
                    expiredEvictions = 0
                )
            ),
            totals = statistics
        )
    }
}
