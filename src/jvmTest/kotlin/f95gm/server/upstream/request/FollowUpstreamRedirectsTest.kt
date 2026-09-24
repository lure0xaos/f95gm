package f95gm.server.upstream.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FollowUpstreamRedirectsTest {
    @Test
    fun acceptsRelativeAndSameHostHttpsRedirects() {
        assertEquals("/sam/latest_alpha/", safeUpstreamRedirect("/sam/latest_alpha/"))
        assertEquals("https://f95zone.to/login/", safeUpstreamRedirect("https://f95zone.to/login/"))
    }

    @Test
    fun rejectsExternalAndProtocolRelativeRedirects() {
        assertNull(safeUpstreamRedirect("https://example.com/steal"))
        assertNull(safeUpstreamRedirect("//example.com/steal"))
        assertNull(safeUpstreamRedirect("javascript:alert(1)"))
    }
}
