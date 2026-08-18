package f95gm.server.routes.session

import f95gm.server.config.session.sessionCookie
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.state.runtime.sessions
import io.ktor.http.*
import io.ktor.server.application.*

internal fun browserSession(call: ApplicationCall): Pair<String, UpstreamSession>? {
    val id = call.request.headers[HttpHeaders.Cookie]
        ?.split(';')
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith("$sessionCookie=") }
        ?.substringAfter('=')
        ?.takeIf { it.isNotBlank() }
    return id?.let { sessions[it]?.let { stored -> it to stored } }
}
