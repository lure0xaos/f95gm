package f95gm.server.http.request

import kotlin.test.Test
import kotlin.test.assertEquals

class SafeHttpsUriTest {
    private val allowedHosts = setOf("f95zone.to", "cdn.f95zone.to")

    @Test
    fun acceptsAllowedHttpsHostsWithDefaultOrExplicitTlsPort() {
        assertEquals(
            "https://f95zone.to/path",
            (SafeHttpsUri.parseResult(
                "https://f95zone.to/path",
                allowedHosts
            ) as SafeHttpsUriResult.Valid).uri.toString()
        )
        assertEquals(
            "https://CDN.F95ZONE.TO:443/media",
            (SafeHttpsUri.parseResult(
                "https://CDN.F95ZONE.TO:443/media",
                allowedHosts
            ) as SafeHttpsUriResult.Valid).uri.toString()
        )
    }

    @Test
    fun rejectsUnsafeSchemesHostsPortsAndUriCredentials() {
        listOf(
            "http://f95zone.to/path",
            "https://untrusted.example/path",
            "https://f95zone.to:8443/path",
            "https://user:secret@f95zone.to/path",
            "https://f95zone.to/path#fragment",
            "//f95zone.to/path"
        ).forEach { value ->
            assertEquals(SafeHttpsUriResult.Invalid::class, SafeHttpsUri.parseResult(value, allowedHosts)::class, value)
        }
    }
}
