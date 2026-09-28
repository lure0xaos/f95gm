package f95gm.server.upstream.request

import f95gm.server.config.upstream.UpstreamBaseUrl
import f95gm.server.http.request.SafeHttpsUri
import f95gm.server.http.request.SafeHttpsUriResult
import f95gm.server.http.request.UriParsing
import f95gm.server.http.request.parseUri
import java.net.URI
import java.net.URISyntaxException

internal sealed interface UpstreamRedirectResult {
    data class Accepted(val path: UpstreamPath) : UpstreamRedirectResult
    data class Rejected(val reason: UpstreamRedirectRejection) : UpstreamRedirectResult
}

internal enum class UpstreamRedirectRejection {
    EMPTY,
    MALFORMED,
    EXTERNAL_HOST,
    UNSAFE_SCHEME,
    PROTOCOL_RELATIVE,
    INVALID_PATH
}

internal class UpstreamRequestValidationException(message: String) : IllegalArgumentException(message)

/** Request URL construction only. Redirect acceptance lives in [UpstreamRedirectPolicy]. */
internal object UpstreamUrlPolicy {
    fun baseUrl(path: UpstreamPath, baseUrl: UpstreamBaseUrl): String {
        if (!path.text.startsWith("/")) throw UpstreamRequestValidationException("Upstream paths must be absolute")
        return baseUrl.value.trimEnd('/') + path.text
    }

    fun safeRequest(path: UpstreamPath, baseUrl: UpstreamBaseUrl): String = try {
        val baseUrlValue = baseUrl.value
        val rawPath = path.text
        val base = URI(baseUrlValue)
        if (rawPath.startsWith("/")) return baseUrl(path, baseUrl)
        if (base.scheme.equals("http", ignoreCase = true) && base.host.isLocalLoopback()) {
            val candidate = URI(rawPath)
            if (!isAllowedLoopbackCandidate(candidate, base)) {
                throw UpstreamRequestValidationException("Upstream URI is not allowed in loopback fixture mode")
            }
            candidate.toString()
        } else {
            when (val result = SafeHttpsUri.parseResult(rawPath, setOf(base.host), setOf(-1, base.port, 443))) {
                is SafeHttpsUriResult.Valid -> result.uri.toString()
                is SafeHttpsUriResult.Invalid -> throw UpstreamRequestValidationException("Upstream URI is not allowed: ${result.name}")
            }
        }
    } catch (_: URISyntaxException) {
        throw UpstreamRequestValidationException("Upstream URI is malformed")
    }
}

/** Pure redirect policy; accepts only same-origin relative paths or same-upstream HTTPS URLs. */
internal object UpstreamRedirectPolicy {
    fun validate(location: String, baseUrl: UpstreamBaseUrl): UpstreamRedirectResult =
        if (location.isBlank()) {
            UpstreamRedirectResult.Rejected(UpstreamRedirectRejection.EMPTY)
        } else {
            safeRedirect(location, baseUrl)?.let(UpstreamRedirectResult::Accepted)
                ?: rejectionFor(location, baseUrl)
        }

    private fun safeRedirect(location: String, baseUrl: UpstreamBaseUrl): UpstreamPath? {
        val uri = when (val parsed = parseUri(location)) {
            is UriParsing.Valid -> parsed.uri
            UriParsing.Invalid -> return null
        }
        return if (!uri.isAbsolute) {
            safeRelativeRedirect(location)
        } else {
            val upstreamUri = URI(baseUrl.value)
            if (upstreamUri.isLocalHttp()) safeLocalRedirect(uri, upstreamUri)
            else safeHttpsRedirect(location, upstreamUri)
        }
    }

    private fun rejectionFor(location: String, baseUrl: UpstreamBaseUrl): UpstreamRedirectResult.Rejected {
        if (location.startsWith("//")) return rejected(UpstreamRedirectRejection.PROTOCOL_RELATIVE)
        if (location.startsWith("javascript:", ignoreCase = true) || location.startsWith("ftp:", ignoreCase = true)) {
            return rejected(UpstreamRedirectRejection.UNSAFE_SCHEME)
        }
        val candidate = when (val parsed = parseUri(location)) {
            is UriParsing.Valid -> parsed.uri
            UriParsing.Invalid -> return rejected(UpstreamRedirectRejection.MALFORMED)
        }
        return if (candidate.isAbsolute) {
            absoluteRejection(candidate, URI(baseUrl.value))
        } else {
            rejected(UpstreamRedirectRejection.INVALID_PATH)
        }
    }

    private fun absoluteRejection(candidate: URI, upstream: URI): UpstreamRedirectResult.Rejected = when {
        candidate.host == null -> rejected(UpstreamRedirectRejection.UNSAFE_SCHEME)
        !candidate.host.equals(upstream.host, ignoreCase = true) -> rejected(UpstreamRedirectRejection.EXTERNAL_HOST)
        candidate.scheme.equals("https", ignoreCase = true) ||
            (upstream.isLocalHttp() && candidate.scheme.equals("http", ignoreCase = true)) ->
            rejected(UpstreamRedirectRejection.INVALID_PATH)
        else -> rejected(UpstreamRedirectRejection.UNSAFE_SCHEME)
    }

    private fun rejected(reason: UpstreamRedirectRejection) = UpstreamRedirectResult.Rejected(reason)

    private fun safeRelativeRedirect(location: String): UpstreamPath? =
        location.takeIf { it.startsWith("/") && !it.startsWith("//") }?.let(::UpstreamPath)

    private fun safeLocalRedirect(uri: URI, upstreamUri: URI): UpstreamPath? = uri.toString().takeIf {
        uri.scheme.equals("http", ignoreCase = true) &&
            uri.host.equals(upstreamUri.host, ignoreCase = true) &&
            uri.port == upstreamUri.port && uri.userInfo == null && uri.fragment == null
    }?.let(::UpstreamPath)

    private fun safeHttpsRedirect(location: String, upstreamUri: URI): UpstreamPath? =
        when (val result = SafeHttpsUri.parseResult(location, setOf(upstreamUri.host), setOf(-1, upstreamUri.port, 443))) {
            is SafeHttpsUriResult.Valid -> UpstreamPath(result.uri.toString())
            is SafeHttpsUriResult.Invalid -> null
        }

    private fun URI.isLocalHttp(): Boolean = scheme.equals("http", ignoreCase = true) && host.isLocalLoopback()
}

internal fun validateUpstreamRedirect(location: String, baseUrl: UpstreamBaseUrl): UpstreamRedirectResult =
    UpstreamRedirectPolicy.validate(location, baseUrl)

private fun isAllowedLoopbackCandidate(candidate: URI, base: URI): Boolean =
    candidate.scheme.equals("http", ignoreCase = true) &&
        candidate.host.equals(base.host, ignoreCase = true) &&
        candidate.port == base.port &&
        candidate.userInfo == null &&
        candidate.fragment == null

private fun String?.isLocalLoopback(): Boolean = this?.lowercase() in setOf("localhost", "127.0.0.1", "::1", "[::1]")
