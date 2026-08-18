package f95gm.server.routes.catalog

import io.ktor.server.application.*

internal fun queryValues(call: ApplicationCall, name: String): List<String> =
    call.request.queryParameters.getAll(name).orEmpty() + call.request.queryParameters.getAll("$name[]").orEmpty()
