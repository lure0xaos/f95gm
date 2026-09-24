package f95gm.server.http.request

import java.net.URI

internal sealed interface SafeHttpsUriResult {
    data class Valid(val uri: URI) : SafeHttpsUriResult
    enum class Invalid : SafeHttpsUriResult {
        MALFORMED,
        UNSAFE_SCHEME,
        DISALLOWED_HOST,
        DISALLOWED_PORT,
        UNSAFE_COMPONENTS
    }
}
