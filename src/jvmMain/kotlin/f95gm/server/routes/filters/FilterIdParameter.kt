package f95gm.server.routes.filters

import f95gm.domain.values.F95FilterId
import f95gm.server.routes.ports.applicationDependencies
import f95gm.server.routes.filters.validation.isValidSavedFilterId
import f95gm.server.http.request.pathParameter
import io.ktor.server.application.*

internal fun ApplicationCall.filterIdParameter(): F95FilterId? =
    pathParameter("filterId")
        ?.takeIf { isValidSavedFilterId(it, applicationDependencies.filterRequestPolicy.maxFilterIdLength.value) }
        ?.let(::F95FilterId)
