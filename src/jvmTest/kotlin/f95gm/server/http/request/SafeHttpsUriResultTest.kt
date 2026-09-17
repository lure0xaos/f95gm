package f95gm.server.http.request

import kotlin.test.Test
import kotlin.test.assertEquals

class SafeHttpsUriResultTest {
    private val hosts = setOf("f95zone.to")

    @Test
    fun classifiesUnsafeUriReasons() {
        assertEquals(SafeHttpsUriResult.Invalid.MALFORMED, SafeHttpsUri.parseResult("not a uri", hosts))
        assertEquals(SafeHttpsUriResult.Invalid.UNSAFE_SCHEME, SafeHttpsUri.parseResult("http://f95zone.to", hosts))
        assertEquals(SafeHttpsUriResult.Invalid.DISALLOWED_HOST, SafeHttpsUri.parseResult("https://example.com", hosts))
        assertEquals(
            SafeHttpsUriResult.Invalid.UNSAFE_COMPONENTS,
            SafeHttpsUri.parseResult("https://f95zone.to/path#fragment", hosts)
        )
        assertEquals(SafeHttpsUriResult.Valid::class, SafeHttpsUri.parseResult("https://f95zone.to/path", hosts)::class)
    }
}
