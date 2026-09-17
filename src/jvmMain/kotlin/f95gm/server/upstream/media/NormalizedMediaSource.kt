package f95gm.server.upstream.media

import f95gm.server.model.media.MediaUrl
import f95gm.server.config.upstream.mediaHosts
import f95gm.server.config.upstream.UpstreamBaseUrl
import f95gm.server.http.request.UriParsing
import f95gm.server.http.request.parseUri
import f95gm.server.upstream.request.UpstreamUrlPolicy
import f95gm.server.upstream.request.UpstreamPath

internal fun normalizedMediaUrl(rawValue: String, baseUrl: UpstreamBaseUrl): MediaUrl? {
    val value = when {
        rawValue.trim().startsWith("//") -> "https:${rawValue.trim()}"
        rawValue.trim().startsWith("/") -> UpstreamUrlPolicy.baseUrl(UpstreamPath(rawValue.trim()), baseUrl)
        else -> rawValue.trim()
    }
    val uri = when (val parsed = parseUri(value)) {
        is UriParsing.Valid -> parsed.uri
        UriParsing.Invalid -> null
    }
    return value.takeIf {
        uri?.scheme.equals("https", ignoreCase = true) && uri?.host?.lowercase() in mediaHosts
    }?.let(::MediaUrl)
}
