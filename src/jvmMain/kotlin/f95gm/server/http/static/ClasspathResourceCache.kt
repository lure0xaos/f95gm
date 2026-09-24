package f95gm.server.http.static

import f95gm.server.state.runtime.cache.BoundedTtlCache

internal class ClasspathResourceCache {
    private val cache = BoundedTtlCache<String, StaticResource?>(512, 300_000L)

    fun get(path: String): StaticResource? = cache.getOrPut(path) {
        Thread.currentThread().contextClassLoader
            .getResourceAsStream("static/$path")
            ?.use { stream -> stream.readBytes().let { bytes -> StaticResource(bytes, etag(bytes)) } }
    }

    fun clear() = cache.clear()
}
