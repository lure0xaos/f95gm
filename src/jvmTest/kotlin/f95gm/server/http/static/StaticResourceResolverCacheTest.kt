package f95gm.server.http.static

import f95gm.server.cache.CacheNamespaces
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StaticResourceResolverCacheTest {
    @Test
    fun detectsSameSizeOverrideChangesWithinOneMillisecondAndReportsOverrideMetrics() {
        val root = Files.createTempDirectory("static-resource-cache")
        val file = root.resolve("asset.js")
        val resolver = StaticResourceResolver(root.toString())
        try {
            Files.write(file, "one".encodeToByteArray())
            val originalTime = FileTime.fromMillis(1_700_000_000_000L)
            Files.setLastModifiedTime(file, originalTime)

            val first = assertIs<StaticResourceLoadResult.Found>(resolver.resolve(StaticResourcePath("asset.js"))).resource
            val observedTime = Files.getLastModifiedTime(file)
            val updatedTime = FileTime.from(observedTime.toInstant().plusNanos(500_000))
            assertEquals(observedTime.toMillis(), updatedTime.toMillis())
            assertNotEquals(observedTime, updatedTime)

            Files.write(file, "two".encodeToByteArray())
            Files.setLastModifiedTime(file, updatedTime)
            val second = assertIs<StaticResourceLoadResult.Found>(resolver.resolve(StaticResourcePath("asset.js"))).resource
            assertNotEquals(first.etag, second.etag)
            assertEquals(second, assertIs<StaticResourceLoadResult.Found>(resolver.resolve(StaticResourcePath("asset.js"))).resource)

            val overrideStats = resolver.snapshot().namespaces.getValue(CacheNamespaces.STATIC)
            assertEquals(1L, overrideStats.entries)
            assertEquals(1L, overrideStats.hits)
            assertEquals(2L, overrideStats.misses)
            assertTrue(resolver.snapshot().countersConsistent)
        } finally {
            resolver.clear()
            Files.deleteIfExists(file)
            Files.deleteIfExists(root)
        }
    }
}
