package f95gm.server.http.static

import io.ktor.http.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class StaticPathTest {
    @Test
    fun rejectsTraversalAndAbsolutePaths() {
        assertNull(safeStaticPath("../secret.txt"))
        assertNull(safeStaticPath("%2e%2e/secret.txt"))
        assertNull(safeStaticPath("C:/secret.txt"))
    }

    @Test
    fun normalizesSafePaths() {
        assertEquals("assets/app.js", safeStaticPath("assets/./app.js")?.value)
        assertEquals("index.html", safeStaticPath("")?.value)
    }

    @Test
    fun etagChangesWhenStaticContentChanges() {
        val first = etag("one".encodeToByteArray())
        val second = etag("two".encodeToByteArray())

        assertEquals(66, first.length)
        assertNotEquals(first, second)
    }

    @Test
    fun mapsKnownExtensionsCaseInsensitivelyAndUsesBinaryFallback() {
        assertEquals(ContentType.Text.Html, staticContentType("HTML"))
        assertEquals(ContentType.Application.JavaScript, staticContentType("js"))
        assertEquals(ContentType.Text.CSS, staticContentType("css"))
        assertEquals(ContentType.Application.Json, staticContentType("map"))
        assertEquals(ContentType.Image.PNG, staticContentType("png"))
        assertEquals(ContentType.Application.OctetStream, staticContentType("wasm"))
    }

    @Test
    fun rejectsNullBytesAndEncodedSeparators() {
        assertNull(safeStaticPath("assets/app.js\u0000"))
        assertNull(safeStaticPath("assets%2fapp.js"))
        assertNull(safeStaticPath("assets%5capp.js"))
    }

    @Test
    fun rejectsMissingOrNonFileOverrideTargets() {
        assertEquals(StaticFileResolution.Missing, resolveOverrideRoot("C:/path/that/does/not/exist"))
        assertEquals(
            StaticFileResolution.Missing,
            resolveOverrideFile(Path.of("C:/path/that/does/not/exist"), "index.html")
        )
    }

    @Test
    fun choosesCachePolicyByAssetRole() {
        assertEquals("no-cache", staticCacheControl("index.html", emptySet()))
        assertEquals(
            "public, max-age=31536000, immutable",
            staticCacheControl("app.js", setOf("app.js"))
        )
        assertEquals("public, max-age=0, must-revalidate", staticCacheControl("favicon.png", emptySet()))
    }

    @Test
    fun normalizationNeverEscapesTheStaticRootForNestedInputs() {
        listOf("../x", "a/../../x", "%2e%2e/x", "a%2fb").forEach { path ->
            assertNull(safeStaticPath(path), path)
        }
    }

    @Test
    fun filesystemSecurityAndIoFailuresRemainDistinguishableFromMissingResources() {
        val securityFailure = object : StaticFileSystem {
            override fun resolveRoot(directory: String) = StaticFileResolution.Failed(StaticFileFailure.SECURITY)
            override fun resolveFile(root: Path, requested: String) = StaticFileResolution.Failed(StaticFileFailure.SECURITY)
        }
        val ioFailure = object : StaticFileSystem {
            override fun resolveRoot(directory: String) = StaticFileResolution.Failed(StaticFileFailure.IO)
            override fun resolveFile(root: Path, requested: String) = StaticFileResolution.Failed(StaticFileFailure.IO)
        }

        assertEquals(StaticFileFailure.SECURITY, (resolveOverrideRoot("static", securityFailure) as StaticFileResolution.Failed).failure)
        assertEquals(StaticFileFailure.SECURITY, (resolveOverrideFile(Path.of("static"), "index.html", securityFailure) as StaticFileResolution.Failed).failure)
        assertEquals(StaticFileFailure.IO, (resolveOverrideRoot("static", ioFailure) as StaticFileResolution.Failed).failure)
        assertEquals(StaticFileFailure.IO, (resolveOverrideFile(Path.of("static"), "index.html", ioFailure) as StaticFileResolution.Failed).failure)
    }
}
