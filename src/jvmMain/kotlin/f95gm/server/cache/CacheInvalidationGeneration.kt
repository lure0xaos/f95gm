package f95gm.server.cache

/** Tracks active loads so targeted invalidation only suppresses matching completions. */
internal class CacheInvalidationGeneration<K> {
    private var generation = 0L
    private val activeLoads = mutableSetOf<CacheGeneration<K>>()

    @Synchronized
    fun begin(key: K, namespace: CacheNamespace): CacheGeneration<K> =
        CacheGeneration(key, namespace, generation).also(activeLoads::add)

    @Synchronized
    fun finish(load: CacheGeneration<K>) {
        activeLoads.remove(load)
    }

    @Synchronized
    fun isCurrent(load: CacheGeneration<K>): Boolean =
        generation == load.globalGeneration && !load.invalidated

    @Synchronized
    fun invalidateKey(key: K) {
        activeLoads.filter { it.key == key }.forEach(CacheGeneration<K>::invalidate)
    }

    @Synchronized
    fun invalidateMatching(predicate: (K) -> Boolean) {
        activeLoads.filter { predicate(it.key) }.forEach(CacheGeneration<K>::invalidate)
    }

    @Synchronized
    fun invalidateNamespace(namespace: CacheNamespace) {
        activeLoads.filter { it.namespace == namespace }.forEach(CacheGeneration<K>::invalidate)
    }

    @Synchronized
    fun invalidateAll() {
        generation++
    }
}

internal class CacheGeneration<K>(
    val key: K,
    val namespace: CacheNamespace,
    val globalGeneration: Long
) {
    @Volatile
    var invalidated: Boolean = false
        private set

    fun invalidate() {
        invalidated = true
    }
}
