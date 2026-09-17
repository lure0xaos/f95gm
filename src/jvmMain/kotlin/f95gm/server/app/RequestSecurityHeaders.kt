package f95gm.server.app

import f95gm.server.config.security.ServerSecurityConfig
import io.ktor.server.application.*

internal fun ApplicationCall.appendSecurityHeaders() {
    val nonce = ServerSecurityConfig.nonce()
    attributes.put(ServerSecurityConfig.CSP_NONCE_ATTRIBUTE, nonce)
    ServerSecurityConfig.headers(nonce).forEach { header -> response.headers.append(header.name, header.value) }
}

