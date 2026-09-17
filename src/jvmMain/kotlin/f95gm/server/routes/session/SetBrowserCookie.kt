package f95gm.server.routes.session

import f95gm.domain.values.F95SessionId
import f95gm.server.config.ServerDefaults
import f95gm.server.config.protocol.SessionProtocolConfig
import f95gm.server.config.security.ServerSecurityConfig
import io.ktor.http.*
import io.ktor.server.application.*

internal fun setBrowserCookie(call: ApplicationCall, id: F95SessionId) {
    call.response.headers.append(
        HttpHeaders.SetCookie,
        "${SessionProtocolConfig.BROWSER_COOKIE}=${id.value}; ${ServerSecurityConfig.COOKIE_ATTRIBUTES}; Max-Age=${ServerDefaults.SESSION_MAX_AGE_SECONDS}"
    )
}
