package f95gm.server.http.response

import f95gm.server.application.port.CachedResponse
import f95gm.server.http.serialization.upstreamJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal suspend fun respond(call: ApplicationCall, status: HttpStatusCode, body: String, contentType: ContentType? = null) {
    call.appendNoStore()
    call.respondText(
        text = body,
        contentType = contentType ?: ContentType.Application.Json,
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
    maxAge: Duration = 60.seconds
) {
    val body = encodeJson(value)
    val etag = EntityTagPolicy.forBody(body)
    call.response.headers.append(HttpHeaders.ETag, etag)
    call.appendPrivateCache(maxAge)
    if (EntityTagPolicy.matches(call, etag)) {
        call.respond(HttpStatusCode.NotModified)
    } else {
        call.respondText(body, ContentType.Application.Json, status)
    }
}

internal suspend fun respondCached(call: ApplicationCall, cached: CachedResponse) {
    respond(
        call,
        HttpStatusCode.fromValue(cached.status.value),
        cached.body,
        cached.contentType?.let(ContentType::parse)
    )
}
