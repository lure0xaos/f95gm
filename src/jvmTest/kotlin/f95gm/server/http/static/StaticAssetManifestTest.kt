package f95gm.server.http.static

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticAssetManifestTest {
    @Test
    fun loadsManifestFromTheInjectedClassLoader() {
        val cache = StaticResourceCache(classLoader = classLoader("{\"index.html\":{},\"app.js\":{}}"))

        assertEquals(setOf("index.html", "app.js"), cache.assetManifestPaths())
        assertEquals(StaticAssetManifestInitializationStatus.LOADED, cache.assetManifestInitializationStatus)
    }

    @Test
    fun recordsMissingAndInvalidManifestStates() {
        val missing = StaticResourceCache(classLoader = classLoader(null))
        val invalid = StaticResourceCache(classLoader = classLoader("not-json"))

        assertTrue(missing.assetManifestPaths().isEmpty())
        assertEquals(StaticAssetManifestInitializationStatus.MISSING, missing.assetManifestInitializationStatus)
        assertTrue(invalid.assetManifestPaths().isEmpty())
        assertEquals(StaticAssetManifestInitializationStatus.INVALID, invalid.assetManifestInitializationStatus)
    }

    @Test
    fun doesNotLoadManifestAfterClose() {
        val cache = StaticResourceCache(classLoader = classLoader("{\"index.html\":{}}"))
        cache.close()

        assertTrue(cache.assetManifestPaths().isEmpty())
        assertEquals(StaticAssetManifestInitializationStatus.UNINITIALIZED, cache.assetManifestInitializationStatus)
    }

    private fun classLoader(content: String?): ClassLoader = object : ClassLoader(null) {
        override fun getResourceAsStream(name: String) =
            if (name == "static/asset-manifest.json") content?.byteInputStream() else null
    }
}
