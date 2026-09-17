package f95gm.server.config.security

import java.security.SecureRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ServerSecurityConfigTest {
    @Test
    fun createsHighEntropyNonceAndEmbedsItInThePolicy() {
        val nonce = ServerSecurityConfig.nonce(SecureRandom())
        val policy = ServerSecurityConfig.contentSecurityPolicy(nonce)

        assertEquals(43, nonce.length)
        assertTrue(nonce.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertTrue(policy.contains("style-src 'self' 'nonce-$nonce'"))
        assertTrue(policy.contains("script-src 'self' 'nonce-$nonce'"))
        assertNotEquals(nonce, ServerSecurityConfig.nonce(SecureRandom()))
    }

    @Test
    fun policyKeepsTrustedTypesAndExcludesInlineExecutionFallbacks() {
        val policy = ServerSecurityConfig.contentSecurityPolicy("test_nonce")

        assertTrue(policy.startsWith("default-src 'self'"))
        assertTrue(policy.contains("img-src 'self' https: data:"))
        assertTrue(policy.contains("frame-ancestors 'none'"))
        assertTrue("unsafe-inline" !in policy)
        assertEquals("Path=/; HttpOnly; SameSite=Lax", ServerSecurityConfig.COOKIE_ATTRIBUTES)
    }
}
