package f95gm.server.routes.catalog

import f95gm.domain.api.ApiQueryParameter
import io.ktor.server.application.*
import f95gm.server.http.request.valuesParameter

internal fun queryValues(call: ApplicationCall, parameter: ApiQueryParameter): List<String> =
    call.request.valuesParameter(parameter)

/** Shared typed access for optional scalar query parameters. */
internal fun queryValue(call: ApplicationCall, parameter: ApiQueryParameter): String? =
    queryValues(call, parameter).firstOrNull()
