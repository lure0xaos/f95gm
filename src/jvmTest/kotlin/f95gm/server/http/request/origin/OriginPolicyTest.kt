package f95gm.server.http.request.origin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OriginPolicyTest {
    @Test
    fun acceptsMatchingHostAndEffectivePort() {
        val https = OriginPolicy("example.test", 443)
        assertTrue(https.accepts("https://example.test"))
        assertTrue(https.accepts("HTTPS://EXAMPLE.TEST:443/path"))

        val http = OriginPolicy("example.test", 8080)
        assertTrue(http.accepts("http://example.test:8080"))
    }

    @Test
    fun acceptsLocalhostLoopbackAliasButRejectsOriginMismatches() {
        val policy = OriginPolicy("localhost", 8080)
        assertTrue(policy.accepts("http://127.0.0.1:8080"))
        assertFalse(policy.accepts("http://127.0.0.1"))
        assertFalse(policy.accepts("https://localhost:8443"))
        assertFalse(policy.accepts("http://other.test:8080"))
        assertFalse(policy.accepts("ftp://localhost:8080"))
        assertFalse(policy.accepts("not a uri"))
    }
}
