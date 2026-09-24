package f95gm.server.http.response

import f95gm.server.state.runtime.IdempotencyRegistry
import f95gm.server.state.runtime.upstreamJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

internal suspend fun respond(call: ApplicationCall, status: HttpStatusCode, body: String, contentType: String? = null) {
    call.response.headers.append(HttpHeaders.CacheControl, "no-store")
    call.respondText(
        text = body,
        contentType = contentType?.let { ContentType.parse(it) } ?: ContentType.Application.Json,
        status = status
    )
}

internal suspend inline fun <reified T> respondJson(call: ApplicationCall, status: HttpStatusCode, value: T) {
    respond(call, status, encodeJson(value))
}

internal inline fun <reified T> encodeJson(value: T): String = upstreamJson.encodeToString(value)

/** Sends a representation with a stable validator and handles conditional GETs. */
internal suspend inline fun <reified T> respondJsonCacheable(
    call: ApplicationCall,
    status: HttpStatusCode,
    value: T,
    maxAgeSeconds: Long = 60
) {
    val body = encodeJson(value)
    val etag = EntityTagPolicy.forBody(body)
    call.response.headers.append(HttpHeaders.ETag, etag)
    call.response.headers.append(HttpHeaders.CacheControl, "private, max-age=${maxAgeSeconds.coerceAtLeast(0)}")
    if (EntityTagPolicy.matches(call, etag)) {
        call.respond(HttpStatusCode.NotModified)
    } else {
        call.respondText(body, ContentType.Application.Json, status)
    }
}

internal suspend fun respondCached(call: ApplicationCall, cached: IdempotencyRegistry.CachedResponse) {
    respond(call, HttpStatusCode.fromValue(cached.status), cached.body, cached.contentType)
}
