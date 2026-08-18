package f95gm.server.routes.session

import f95gm.server.config.session.sessionCookie
import io.ktor.http.*
import io.ktor.server.application.*

internal fun clearBrowserCookie(call: ApplicationCall) {
    call.response.headers.append(
        HttpHeaders.SetCookie,
        "$sessionCookie=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
    )
}
