package f95gm.server.cache

import f95gm.server.kernel.AwaitableLifecycleResource

/** Read-only cache operations needed by request handlers. */
internal interface CacheReader<K, V> {
    fun getOrPut(key: K, factory: () -> V): V
    suspend fun getOrPutSuspending(key: K, factory: suspend () -> V): V
}

/** Mutation operations kept separate from cache reads and statistics. */
internal interface CacheInvalidator<K> {
    fun invalidate(key: K)
    fun invalidateIf(predicate: (K) -> Boolean): Int
    fun invalidateNamespace(namespace: CacheNamespace): Int
    fun invalidateAll()
    fun clear()
}

internal interface CacheAccess<K, V> : CacheReader<K, V>, CacheInvalidator<K>

/** Owning view of a cache; request handlers should depend on [CacheAccess] instead. */
internal interface ManagedCache<K, V> : CacheAccess<K, V>, CacheStatisticsProvider, AwaitableLifecycleResource
