package f95gm.server.routes.session

import f95gm.server.config.session.sessionCookie
import f95gm.server.config.session.sessionMaxAgeSeconds
import io.ktor.http.*
import io.ktor.server.application.*

internal fun setBrowserCookie(call: ApplicationCall, id: String) {
    call.response.headers.append(
        HttpHeaders.SetCookie,
        "$sessionCookie=$id; Path=/; Max-Age=$sessionMaxAgeSeconds; HttpOnly; SameSite=Lax"
    )
}
