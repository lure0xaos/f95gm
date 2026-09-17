package f95gm.server.routes.session

import f95gm.server.config.protocol.SessionProtocolConfig
import f95gm.server.config.security.ServerSecurityConfig
import io.ktor.http.*
import io.ktor.server.application.*

internal fun clearBrowserCookie(call: ApplicationCall) {
    call.response.headers.append(
        HttpHeaders.SetCookie,
        "${SessionProtocolConfig.BROWSER_COOKIE}=; ${ServerSecurityConfig.COOKIE_ATTRIBUTES}; Max-Age=0"
    )
}
