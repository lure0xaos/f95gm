package f95gm.server.routes.session

import f95gm.domain.values.F95SessionId
import f95gm.server.config.security.ServerSecurityConfig
import f95gm.server.config.session.sessionCookie
import f95gm.server.config.session.sessionMaxAgeSeconds
import io.ktor.http.*
import io.ktor.server.application.*

internal fun setBrowserCookie(call: ApplicationCall, id: F95SessionId) {
    call.response.headers.append(
        HttpHeaders.SetCookie,
        "$sessionCookie=${id.value}; ${ServerSecurityConfig.COOKIE_ATTRIBUTES}; Max-Age=$sessionMaxAgeSeconds"
    )
}
