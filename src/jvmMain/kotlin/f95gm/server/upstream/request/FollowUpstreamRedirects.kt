package f95gm.server.upstream.request

import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.upstream.upstream
import f95gm.server.http.request.SafeHttpsUri
import f95gm.server.http.request.SafeHttpsUriResult
import f95gm.server.http.response.isRedirect
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.state.runtime.RouteDependencies
import io.ktor.http.*
import java.net.URI
import java.net.URISyntaxException

internal suspend fun followUpstreamRedirects(
    context: RouteDependencies,
    initial: UpstreamResponse,
    session: UpstreamSession
): UpstreamResponse {
    var response = initial
    repeat(ServerConstants.MAX_UPSTREAM_REDIRECTS) {
        if (!response.status.isRedirect) return response
        val location = response.location?.takeIf { it.isNotBlank() } ?: return response
        val safeLocation = safeUpstreamRedirect(location) ?: return response
        response = context.upstreamRequest(HttpMethod.Get, safeLocation, session)
    }
    return response
}

internal fun safeUpstreamRedirect(location: String): String? = try {
    val uri = URI(location)
    if (!uri.isAbsolute) {
        safeRelativeRedirect(location)
    } else {
        val upstreamUri = URI(upstream)
        if (upstreamUri.isLocalHttp()) safeLocalRedirect(uri, upstreamUri)
        else safeHttpsRedirect(location, upstreamUri)
    }
} catch (_: URISyntaxException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

private fun safeRelativeRedirect(location: String): String? =
    location.takeIf { it.startsWith("/") && !it.startsWith("//") }

private fun safeLocalRedirect(uri: URI, upstreamUri: URI): String? = uri.toString().takeIf {
    uri.scheme.equals("http", ignoreCase = true) &&
            uri.host.equals(upstreamUri.host, ignoreCase = true) &&
            uri.port == upstreamUri.port && uri.userInfo == null && uri.fragment == null
}

private fun safeHttpsRedirect(location: String, upstreamUri: URI): String? =
    when (val result = SafeHttpsUri.parseResult(location, setOf(upstreamUri.host), setOf(-1, upstreamUri.port, 443))) {
        is SafeHttpsUriResult.Valid -> result.uri.toString()
        is SafeHttpsUriResult.Invalid -> null
    }

private fun URI.isLocalHttp(): Boolean =
    scheme.equals("http", ignoreCase = true) && host.isLocalLoopback()

private fun String?.isLocalLoopback(): Boolean = this?.lowercase() in setOf("localhost", "127.0.0.1", "::1", "[::1]")
