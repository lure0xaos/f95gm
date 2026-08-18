package f95gm.server.app

import f95gm.server.config.constants.ServerConstants
import io.ktor.http.*
import io.ktor.server.application.*

internal fun setShutdownCookie(call: ApplicationCall, token: String) {
    call.response.headers.append(
        HttpHeaders.SetCookie,
        "${ServerConstants.SHUTDOWN_COOKIE}=$token; Path=/; HttpOnly; SameSite=Strict"
    )
}
