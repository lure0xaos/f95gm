package f95gm.server.http.request

import f95gm.domain.values.F95FilterId
import f95gm.server.routes.filters.validation.isValidSavedFilterId
import io.ktor.server.application.*

internal fun ApplicationCall.filterIdParameter(): F95FilterId? =
    parameters["filterId"]?.takeIf(::isValidSavedFilterId)?.let(::F95FilterId)
