package f95gm.server.upstream.html

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SanitizeThreadHtmlTest {
    @Test
    fun removesExecutableElementsAttributesAndUnsafeStyles() {
        val sanitized = sanitizeThreadHtml(
            """
                <p onclick="alert(1)" style="background:url(https://evil.example/x)">Text</p>
                <script>alert(1)</script><iframe src="https://evil.example/frame"></iframe>
                <a href="javascript:alert(1)">Unsafe</a><img src="data:image/png;base64,abc">
            """.trimIndent()
        ).value

        assertTrue(sanitized.contains("Text"))
        assertFalse(sanitized.contains("<script", ignoreCase = true))
        assertFalse(sanitized.contains("<iframe", ignoreCase = true))
        assertFalse(sanitized.contains("onclick", ignoreCase = true))
        assertFalse(sanitized.contains("javascript:", ignoreCase = true))
        assertFalse(sanitized.contains("data:image/", ignoreCase = true))
        assertFalse(sanitized.contains("style=", ignoreCase = true))
    }

    @Test
    fun promotesDataSourceAndProxiesApprovedMediaWithLazyLoading() {
        val sanitized = sanitizeThreadHtml(
            """
                <a href="//attachments.f95zone.to/files/game.jpg">
                    <img data-src="//attachments.f95zone.to/files/game.jpg" alt="Game">
                </a>
            """.trimIndent()
        ).value

        assertTrue(sanitized.contains("href=\"/api/media?url=https%3A%2F%2Fattachments.f95zone.to%2Ffiles%2Fgame.jpg\""))
        assertTrue(sanitized.contains("src=\"/api/media?url=https%3A%2F%2Fattachments.f95zone.to%2Ffiles%2Fgame.jpg\""))
        assertTrue(sanitized.contains("loading=\"lazy\""))
        assertTrue(sanitized.contains("decoding=\"async\""))
        assertFalse(sanitized.contains("data-src"))
    }
}
