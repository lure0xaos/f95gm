package f95gm.server.http.request

import io.ktor.http.ContentType

/** Converts untrusted response metadata into a typed value without leaking parser exceptions. */
internal fun parseContentTypeOrNull(raw: String): ContentType? = try {
    ContentType.parse(raw)
} catch (_: IllegalArgumentException) {
    null
}
