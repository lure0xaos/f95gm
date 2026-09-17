package f95gm.server.http.response

import io.ktor.http.*
import io.ktor.server.application.*
import java.security.MessageDigest

internal object EntityTagPolicy {
    fun forBody(body: String): String = "\"${sha256Hex(body)}\""

    /** If-None-Match uses weak comparison for GET/HEAD validators. */
    fun matches(call: ApplicationCall, etag: String): Boolean =
        call.request.headers[HttpHeaders.IfNoneMatch]
            ?.split(',')
            ?.asSequence()
            ?.map(String::trim)
            ?.any { it == "*" || it.removePrefix("W/") == etag.removePrefix("W/") }
            ?: false

    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
