package f95gm.server.upstream.request

import f95gm.server.config.upstream.UpstreamBaseUrl
import kotlin.test.Test
import kotlin.test.assertIs

class UpstreamInputPropertyTest {
    private val baseUrl = UpstreamBaseUrl.parse("https://f95zone.to")

    @Test
    fun redirectValidationRejectsARepresentativeGeneratedInputSet() {
        val invalid = listOf(
            "javascript:alert(1)", "//evil.example/path", "https://evil.example/path",
            "http://f95zone.to/path", "ftp://f95zone.to/path", "https://f95zone.to.evil/path"
        )
        invalid.forEach { assertIs<UpstreamRedirectResult.Rejected>(validateUpstreamRedirect(it, baseUrl), it) }
        listOf("/", "/sam/latest_alpha/", "https://f95zone.to/login/").forEach {
            assertIs<UpstreamRedirectResult.Accepted>(validateUpstreamRedirect(it, baseUrl), it)
        }
    }
}
