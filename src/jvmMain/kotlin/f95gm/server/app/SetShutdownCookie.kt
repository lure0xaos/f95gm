package f95gm.server.app

import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.request.ShutdownToken
import io.ktor.http.*
import io.ktor.server.application.*

internal fun setShutdownCookie(call: ApplicationCall, token: ShutdownToken) {
    call.response.headers.append(
        HttpHeaders.SetCookie,
        "${ServerConstants.SHUTDOWN_COOKIE}=${token.value}; Path=/; HttpOnly; SameSite=Strict"
    )
}
