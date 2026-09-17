package f95gm.server.cache

import f95gm.server.kernel.MonotonicMillis
import f95gm.server.kernel.AccessOrderedStore

/** Lock-free-in-itself cache storage; callers provide the cache's synchronization boundary. */
internal class CacheStore<K, V> {
    private val entries = AccessOrderedStore<K, CacheEntry<V>>()
    private var accessSequence = 0L

    fun get(key: K): CacheEntry<V>? = entries[key]?.also { entry ->
        entries.put(key, entry.copy(lastAccess = AccessSequence(nextAccessSequence())))
    }

    fun keys(): Set<K> = entries.snapshot().mapTo(linkedSetOf()) { it.key }

    fun entries(): Collection<Map.Entry<K, CacheEntry<V>>> = entries.snapshot().map { entry ->
        java.util.AbstractMap.SimpleImmutableEntry(entry.key, entry.value)
    }

    fun remove(key: K): CacheEntry<V>? = entries.remove(key)

    fun removeIf(predicate: (K) -> Boolean): List<K> = entries.snapshot().map { it.key }.filter(predicate).also { it.forEach(entries::remove) }

    fun clear() = entries.clear()

    fun size(): Int = entries.size

    fun put(key: K, value: V, expiresAt: MonotonicMillis) {
        entries.put(key, CacheEntry(value, expiresAt, AccessSequence(nextAccessSequence())))
    }

    private fun nextAccessSequence(): Long {
        accessSequence += 1L
        return accessSequence
    }
}
