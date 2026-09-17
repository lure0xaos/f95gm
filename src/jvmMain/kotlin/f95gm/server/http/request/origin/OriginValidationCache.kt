package f95gm.server.http.request.origin

import f95gm.server.kernel.ServerClock
import f95gm.server.platform.SystemServerClock
import f95gm.server.model.EpochMillis
import f95gm.server.cache.CacheFactory
import f95gm.server.cache.CachePolicy
import f95gm.server.kernel.EntryCapacity
import f95gm.server.cache.CacheNamespaces
import f95gm.server.cache.CachePolicies
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource
import kotlin.time.Duration

internal class OriginValidationCache(
    maxEntries: Int = CachePolicies.ORIGIN.maxEntries,
    ttl: Duration = CachePolicies.ORIGIN.ttl,
    private val clock: ServerClock = SystemServerClock
) : LifecycleResource {
    private val closeGuard = CloseGuard()
    private val values = CacheFactory.create<String, Boolean>(
        CachePolicy.withCapacity(EntryCapacity(maxEntries), ttl),
        clock,
        namespaceOf = { CacheNamespaces.ORIGIN }
    )

    fun getOrPut(key: String, now: EpochMillis = clock.epochMillis(), compute: () -> Boolean): Boolean =
        values.getOrPutAt(key, now.value, compute)

    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce { values.clear() }
}
