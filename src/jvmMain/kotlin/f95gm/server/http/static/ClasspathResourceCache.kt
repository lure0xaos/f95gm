package f95gm.server.http.static

import f95gm.server.cache.CacheFactory
import f95gm.server.cache.CachePolicies
import f95gm.server.cache.CacheNamespaces
import f95gm.server.cache.CacheStatisticsSnapshot
import f95gm.server.cache.CacheStatisticsProvider

internal class ClasspathResourceCache : CacheStatisticsProvider {
    private val cache = CacheFactory.create<String, StaticResource?>(
        CachePolicies.CLASSPATH, namespaceOf = { CacheNamespaces.CLASSPATH }
    )

    fun get(path: String): StaticResource? = cache.getOrPut(path) {
        Thread.currentThread().contextClassLoader
            .getResourceAsStream("static/$path")
            ?.use { stream -> stream.readBytes().let { bytes -> StaticResource(bytes, etag(bytes)) } }
    }

    fun clear() = cache.clear()

    override fun snapshot(): CacheStatisticsSnapshot = cache.snapshot()
}
