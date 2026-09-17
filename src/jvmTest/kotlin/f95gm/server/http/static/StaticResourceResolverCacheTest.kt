package f95gm.server.http.static

import f95gm.server.cache.CacheNamespaces
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StaticResourceResolverCacheTest {
    @Test
    fun resolverReadsAndClosesTheOpenedFileHandle() {
        val content = "stable-handle".encodeToByteArray()
        var openCount = 0
        var readCount = 0
        var closeCount = 0
        val openedPath = Path.of("virtual-root", "asset.js")
        val fileSystem = object : StaticFileSystem {
            override fun resolveRoot(directory: String) = StaticFileResolution.Found(Path.of("virtual-root"))

            override fun resolveFile(root: Path, requested: String): StaticFileResolution =
                error("Resolver must read through the opened file handle")

            override fun openFile(root: Path, requested: String): StaticFileOpenResult {
                openCount++
                return StaticFileOpenResult.Opened(object : StaticFileAccess {
                    override val path = openedPath
                    override val size = content.size.toLong()
                    override val modifiedAt = FileTime.fromMillis(1_700_000_000_000L)
                    override fun readBytes(): ByteArray {
                        readCount++
                        return content
                    }
                    override fun close() { closeCount++ }
                })
            }
        }

        val result = StaticResourceResolver("virtual-root", fileSystem).resolve(StaticResourcePath("asset.js"))

        assertEquals(1, openCount)
        assertEquals(1, readCount)
        assertEquals(1, closeCount)
        assertContentEquals(content, assertIs<StaticResourceLoadResult.Found>(result).resource.bytes)
    }

    @Test
    fun resolverUsesInjectedFilesystemForRootAndFileFailures() {
        var resolvedRoot = false
        var openedFile = false
        val fileSystem = object : StaticFileSystem {
            override fun resolveRoot(directory: String): StaticFileResolution {
                resolvedRoot = true
                return StaticFileResolution.Found(Path.of("virtual-root"))
            }

            override fun resolveFile(root: Path, requested: String): StaticFileResolution =
                error("Resolver should use openFile for override access")

            override fun openFile(root: Path, requested: String): StaticFileOpenResult {
                openedFile = true
                return StaticFileOpenResult.Failed(StaticFileFailure.SECURITY)
            }
        }
        val resolver = StaticResourceResolver("virtual-root", fileSystem)

        val result = resolver.resolve(StaticResourcePath("asset.js"))

        assertTrue(resolvedRoot)
        assertTrue(openedFile)
        assertEquals(StaticResourceLoadResult.Failed(StaticResourceFailure.SECURITY), result)
    }

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
