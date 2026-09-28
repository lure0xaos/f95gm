package f95gm.server.cache

import f95gm.server.kernel.MonotonicMillis
import f95gm.server.kernel.ResourceLock

/** Coordinates synchronized entry access, expiry, and stale-loader suppression. */
internal class CacheEntryCoordinator<K, V>(
    private val store: CacheStore<K, V>,
    private val expirationPolicy: CacheExpirationPolicy,
    private val cacheLock: ResourceLock,
    private val evictionController: CacheEvictionController<K, V>,
    private val invalidationGeneration: CacheInvalidationGeneration
) {
    fun lookup(key: K, now: MonotonicMillis): CacheLookup<V> = cacheLock.withLock {
        evictionController.removeExpired(now)
        val entry = store.get(key)?.takeUnless { expirationPolicy.isExpired(it, now) }
            ?: return@withLock CacheLookup.Miss
        CacheLookup.Hit(entry.value)
    }

    fun storeIfCurrent(
        key: K,
        value: V,
        now: MonotonicMillis,
        insertionNow: () -> MonotonicMillis,
        generation: CacheGeneration
    ): Boolean = cacheLock.withLock {
        if (generation != invalidationGeneration.current()) return@withLock false
        evictionController.evictIfNeeded()
        store.put(key, value, expirationPolicy.expiresAt(insertionNow().coerceAtLeast(now)))
        true
    }

    fun remove(key: K) = cacheLock.withLock { store.remove(key) }

    fun removeIf(predicate: (K) -> Boolean): Int = cacheLock.withLock { store.removeIf(predicate).size }

    fun clear() = cacheLock.withLock { store.clear() }

    fun sweepExpired(now: MonotonicMillis): Int = cacheLock.withLock {
        evictionController.removeExpired(now)
    }

    fun keys(): Set<K> = cacheLock.withLock { store.keys() }

    fun snapshotKeys(now: MonotonicMillis): Set<K> = cacheLock.withLock {
        evictionController.removeExpired(now)
        store.keys()
    }
}
