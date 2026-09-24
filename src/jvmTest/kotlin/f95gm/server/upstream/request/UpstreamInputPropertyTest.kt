package f95gm.server.upstream.request

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpstreamInputPropertyTest {
    @Test
    fun redirectValidationRejectsARepresentativeGeneratedInputSet() {
        val invalid = listOf(
            "javascript:alert(1)", "//evil.example/path", "https://evil.example/path",
            "http://f95zone.to/path", "ftp://f95zone.to/path", "https://f95zone.to.evil/path"
        )
        invalid.forEach { assertNull(safeUpstreamRedirect(it), it) }
        listOf("/", "/sam/latest_alpha/", "https://f95zone.to/login/").forEach {
            assertTrue(safeUpstreamRedirect(it) != null, it)
        }
    }
}
