package f95gm.server.cache

/** Records cache-wide and bounded namespace lookup metrics consistently for all load paths. */
internal class CacheLookupTelemetry<K>(
    private val cache: CacheTelemetry,
    private val namespaces: CacheNamespaceTelemetry,
    private val namespaceOf: (K) -> CacheNamespace
) {
    fun hit(key: K) {
        cache.recordHit()
        namespaces.hit(namespaces.tracked(namespaceOf(key)))
    }

    fun miss(key: K) {
        cache.recordMiss()
        namespaces.miss(namespaces.tracked(namespaceOf(key)))
    }

    fun tracked(key: K): CacheNamespace = namespaces.tracked(namespaceOf(key))
}
