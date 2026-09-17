package f95gm.server.http.request

import java.net.URI
import java.net.URISyntaxException

internal sealed interface UriParsing {
    data class Valid(val uri: URI) : UriParsing
    data object Invalid : UriParsing
}

internal fun parseUri(value: String): UriParsing = try {
    UriParsing.Valid(URI(value))
} catch (_: URISyntaxException) {
    UriParsing.Invalid
} catch (_: IllegalArgumentException) {
    UriParsing.Invalid
}
