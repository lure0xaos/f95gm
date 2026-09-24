package f95gm.server.config.security

import io.ktor.util.*
import java.security.SecureRandom
import java.util.*

internal object ServerSecurityConfig {
    val CSP_NONCE_ATTRIBUTE = AttributeKey<String>("f95gm-csp-nonce")
    private val nonceRandom = SecureRandom()
    const val CONTENT_TYPE_OPTIONS = "nosniff"
    const val REFERRER_POLICY = "strict-origin-when-cross-origin"
    const val FRAME_OPTIONS = "DENY"
    const val PERMISSIONS_POLICY = "camera=(), geolocation=(), microphone=()"

    fun nonce(random: SecureRandom = nonceRandom): String =
        ByteArray(32).also(random::nextBytes).let(Base64.getUrlEncoder().withoutPadding()::encodeToString)

    fun contentSecurityPolicy(nonce: String): String =
        "default-src 'self'; img-src 'self' https: data:; style-src 'self' 'nonce-$nonce'; script-src 'self' 'nonce-$nonce'; frame-ancestors 'none'"

    const val COOKIE_ATTRIBUTES = "Path=/; HttpOnly; SameSite=Lax"
}
