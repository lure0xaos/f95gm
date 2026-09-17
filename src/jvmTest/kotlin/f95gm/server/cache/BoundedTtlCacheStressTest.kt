package f95gm.server.cache

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class BoundedTtlCacheStressTest {
    @Test
    fun arbitraryNamespacesCannotGrowOperationalCardinalityWithoutBound() {
        val cache = BoundedTtlCache<String, Int>(8_192, 60.seconds, namespaceOf = { CacheNamespace(it) })
        repeat(10_000) { index -> cache.getOrPut("namespace-$index") { index } }
        assertTrue(cache.snapshot().namespaces.size <= 257, "namespace diagnostics must remain bounded")
    }
}
