package f95gm.server.cache

import f95gm.server.kernel.MonotonicMillis

/** Owns expiry/capacity decisions so the cache facade coordinates rather than implements them. */
internal class CacheEvictionController<K, V>(
    private val capacity: Int,
    private val store: CacheStore<K, V>,
    private val evictionPolicy: EvictionPolicy<K, CacheEntry<V>>,
    private val namespaceOf: (K) -> CacheNamespace,
    private val telemetry: CacheTelemetry,
    private val namespaceTelemetry: CacheNamespaceTelemetry,
    private val eventSink: CacheEventSink
) {
    fun removeExpired(now: MonotonicMillis): Int {
        val expiredEntries = store.entries().filter { it.value.expiresAt <= now }
        expiredEntries.forEach { entry ->
            store.remove(entry.key)
            val namespace = trackedNamespace(entry.key)
            telemetry.recordEviction(expired = true)
            namespaceTelemetry.eviction(namespace, expired = true)
            eventSink.publish(CacheEvent.Evicted(namespace, expired = true))
        }
        return expiredEntries.size
    }

    fun evictIfNeeded() {
        while (store.size() >= capacity) {
            val keyToRemove = evictionPolicy.select(store.entries()) ?: break
            if (store.remove(keyToRemove) != null) {
                val namespace = trackedNamespace(keyToRemove)
                telemetry.recordEviction(expired = false)
                namespaceTelemetry.eviction(namespace, expired = false)
                eventSink.publish(CacheEvent.Evicted(namespace, expired = false))
            }
        }
    }

    private fun trackedNamespace(key: K): CacheNamespace = namespaceTelemetry.tracked(namespaceOf(key))
}
