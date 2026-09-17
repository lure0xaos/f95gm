package f95gm.server.http.request

import io.ktor.server.application.ApplicationCall

/** Keeps route path lookup at the request-boundary helper instead of scattering raw Ktor access. */
internal fun ApplicationCall.pathParameter(name: String): String? = parameters[name]
