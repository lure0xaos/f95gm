package f95gm.server.http.request

import io.ktor.server.request.*

@JvmInline
internal value class RequestPath(val value: String) {
    init {
        require(value.startsWith('/'))
    }

    companion object {
        fun from(uri: String): RequestPath = RequestPath(uri.substringBefore('?'))
    }
}

internal fun ApplicationRequest.requestPath(): RequestPath = RequestPath.from(uri)
