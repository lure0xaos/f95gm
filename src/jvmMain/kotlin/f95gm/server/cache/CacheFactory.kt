package f95gm.server.cache

import f95gm.server.kernel.ServerClock
import f95gm.server.platform.SystemServerClock

/** Single construction boundary for bounded caches and their operational policies. */
internal object CacheFactory {
    fun <K, V> create(
        policy: CachePolicy,
        clock: ServerClock = SystemServerClock,
        namespaceOf: (K) -> CacheNamespace = { CacheNamespaces.DEFAULT }
    ): BoundedTtlCache<K, V> = BoundedTtlCache.withPolicy(policy, clock, namespaceOf = namespaceOf)

    fun <K, V> createManaged(
        policy: CachePolicy,
        clock: ServerClock = SystemServerClock,
        namespaceOf: (K) -> CacheNamespace = { CacheNamespaces.DEFAULT }
    ): ManagedCache<K, V> = create(policy, clock, namespaceOf)
}
