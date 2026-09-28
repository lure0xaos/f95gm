package f95gm.server.http.request

import java.net.URI

internal class SafeHttpsUri private constructor(val uri: URI) {
    companion object {

        fun parseResult(
            value: String,
            allowedHosts: Set<String>,
            allowedPorts: Set<Int> = setOf(-1, 443)
        ): SafeHttpsUriResult {
            val uri = when (val parsed = parseUri(value)) {
                is UriParsing.Valid -> parsed.uri
                UriParsing.Invalid -> return SafeHttpsUriResult.Invalid.MALFORMED
            }
            if (!uri.scheme.equals("https", ignoreCase = true)) return SafeHttpsUriResult.Invalid.UNSAFE_SCHEME
            if (uri.host == null || uri.host.lowercase() !in allowedHosts.map(String::lowercase)) return SafeHttpsUriResult.Invalid.DISALLOWED_HOST
            if (uri.port !in allowedPorts) return SafeHttpsUriResult.Invalid.DISALLOWED_PORT
            if (uri.userInfo != null || uri.fragment != null) return SafeHttpsUriResult.Invalid.UNSAFE_COMPONENTS
            return SafeHttpsUriResult.Valid(uri)
        }
    }
}
