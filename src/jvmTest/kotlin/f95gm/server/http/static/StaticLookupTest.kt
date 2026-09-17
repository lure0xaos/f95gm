package f95gm.server.http.static

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StaticLookupTest {
    @Test
    fun resolvesOnlyRegularFilesContainedByTheOverrideRoot() {
        val root = Files.createTempDirectory("static-lookup")
        val canonicalRoot = root.toRealPath()
        val nested = Files.createDirectories(root.resolve("assets"))
        val file = nested.resolve("app.js").also { it.writeText("const app = true;") }
        try {
            assertEquals(StaticFileResolution.Found(canonicalRoot), NioStaticFileSystem.resolveRoot(root.toString()))
            assertEquals(StaticFileResolution.Found(file.toRealPath()), NioStaticFileSystem.resolveFile(canonicalRoot, "assets/app.js"))
            assertEquals(StaticFileResolution.Missing, NioStaticFileSystem.resolveFile(canonicalRoot, "../outside.js"))
            assertEquals(StaticFileResolution.Missing, NioStaticFileSystem.resolveFile(canonicalRoot, "assets"))
            assertEquals(StaticFileResolution.Missing, NioStaticFileSystem.resolveRoot(root.resolve("missing").toString()))
        } finally {
            Files.deleteIfExists(file)
            Files.deleteIfExists(nested)
            Files.deleteIfExists(root)
        }
    }

    @Test
    fun openedFileHandleReadsBytesAndCloses() {
        val root = Files.createTempDirectory("static-open")
        val canonicalRoot = root.toRealPath()
        val file = root.resolve("asset.txt").also { it.writeText("served from a stable handle") }
        try {
            val opened = assertIs<StaticFileOpenResult.Opened>(NioStaticFileSystem.openFile(canonicalRoot, "asset.txt"))
            opened.file.use { access ->
                assertEquals(file.toRealPath(), access.path)
                assertEquals(Files.size(file), access.size)
                assertContentEquals(Files.readAllBytes(file), access.readBytes())
            }
            assertEquals(StaticFileOpenResult.Missing, NioStaticFileSystem.openFile(canonicalRoot, "../secret"))
            assertEquals(StaticFileOpenResult.Missing, NioStaticFileSystem.openFile(canonicalRoot, "missing"))
        } finally {
            Files.deleteIfExists(file)
            Files.deleteIfExists(root)
        }
    }

    @Test
    fun invalidPathsAreReportedWithoutEscapingTheFilesystemBoundary() {
        val root = Files.createTempDirectory("static-invalid")
        try {
            assertEquals(
                StaticFileResolution.Failed(StaticFileFailure.INVALID_PATH),
                NioStaticFileSystem.resolveRoot("\u0000")
            )
            assertEquals(
                StaticFileResolution.Failed(StaticFileFailure.INVALID_PATH),
                NioStaticFileSystem.resolveFile(root, "\u0000")
            )
            assertEquals(
                StaticFileOpenResult.Failed(StaticFileFailure.INVALID_PATH),
                NioStaticFileSystem.openFile(root, "\u0000")
            )
        } finally {
            Files.deleteIfExists(root)
        }
    }
}
