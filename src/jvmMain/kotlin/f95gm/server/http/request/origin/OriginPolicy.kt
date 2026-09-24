package f95gm.server.http.request.origin

import java.net.URI
import java.net.URISyntaxException

internal class OriginPolicy(
    private val expectedHost: String,
    private val expectedPort: Int
) {
    fun accepts(origin: String): Boolean = try {
        val uri = URI(origin)
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        val localHost = expectedHost.lowercase()
        val hostMatches = host == localHost ||
                (localHost == "localhost" && host == "127.0.0.1") ||
                (localHost == "127.0.0.1" && host == "localhost") ||
                (localHost == "::1" && host == "::1")
        val port = if (uri.port == -1) if (scheme == "https") 443 else 80 else uri.port
        hostMatches && port == expectedPort
    } catch (_: URISyntaxException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
}
