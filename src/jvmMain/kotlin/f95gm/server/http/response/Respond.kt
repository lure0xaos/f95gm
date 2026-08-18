package f95gm.server.http.response

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

internal suspend fun respond(call: ApplicationCall, status: Int, body: String, contentType: String? = null) {
    call.respondText(
        text = body,
        contentType = contentType?.let { ContentType.parse(it) } ?: ContentType.Application.Json,
        status = HttpStatusCode.fromValue(status)
    )
}
