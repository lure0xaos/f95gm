package f95gm.server.config.upstream

import java.net.URI
import java.net.URISyntaxException

internal class UpstreamConfigurationViolation(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

@JvmInline
internal value class UpstreamBaseUrl private constructor(val value: String) {
    companion object {
        fun parse(raw: String): UpstreamBaseUrl {
            val configured = raw.trim()
            val uri = parseUri(configured)
            validateScheme(uri)
            validateOriginShape(uri)
            return UpstreamBaseUrl("${uri.scheme.lowercase()}://${uri.rawAuthority}")
        }

        private fun parseUri(configured: String): URI = try {
            require(configured.isNotBlank()) { "Upstream base URL must not be blank" }
            URI(configured)
        } catch (error: URISyntaxException) {
            throw UpstreamConfigurationViolation("Invalid F95zone upstream base URL", error)
        }

        private fun validateScheme(uri: URI) {
            val httpsOrigin = uri.isAbsolute && uri.host != null && uri.scheme.equals("https", ignoreCase = true)
            val loopbackHttp = uri.scheme.equals("http", ignoreCase = true) &&
                uri.host?.lowercase() in setOf("localhost", "127.0.0.1", "::1", "[::1]")
            require(httpsOrigin || loopbackHttp) {
                "F95GM upstream overrides must use HTTPS (HTTP is permitted only for loopback test fixtures)"
            }
        }

        private fun validateOriginShape(uri: URI) {
            val hasForbiddenParts = uri.userInfo != null || uri.query != null || uri.fragment != null
            val hasNonRootPath = !uri.path.isNullOrEmpty() && uri.path != "/"
            require(!hasForbiddenParts && !hasNonRootPath) {
                "F95GM upstream base URL must be an origin without credentials, query, or fragment"
            }
        }
    }
}

/** Immutable endpoint set captured with the server configuration snapshot. */
internal data class UpstreamEndpointConfig(val baseUrl: UpstreamBaseUrl) {
    val origin: String get() = baseUrl.value
    val latestPage: String get() = "$origin/sam/latest_alpha/"
    val latestApi: String get() = "${latestPage}latest_data.php"

    companion object {
        val DEFAULT = UpstreamEndpointConfig(UpstreamBaseUrl.parse("https://f95zone.to"))
        fun from(raw: String): UpstreamEndpointConfig = UpstreamEndpointConfig(UpstreamBaseUrl.parse(raw))
    }
}
