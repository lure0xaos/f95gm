package f95gm.server.http.response

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import kotlin.time.Duration

internal fun ApplicationCall.appendNoStore() {
    response.headers.append(HttpHeaders.CacheControl, "no-store")
}

internal fun ApplicationCall.appendPrivateCache(maxAge: Duration) {
    response.headers.append(
        HttpHeaders.CacheControl,
        "private, max-age=${maxAge.inWholeSeconds.coerceAtLeast(0)}"
    )
}
