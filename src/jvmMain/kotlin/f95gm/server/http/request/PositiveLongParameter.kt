package f95gm.server.http.request

import f95gm.domain.values.F95ThreadId
import io.ktor.server.application.*

internal fun ApplicationCall.positiveLongParameter(name: String): String? =
    parameters[name]?.toLongOrNull()?.takeIf { it > 0 }?.toString()

internal fun ApplicationCall.positiveThreadIdParameter(name: String): F95ThreadId? =
    positiveLongParameter(name)?.let(::F95ThreadId)
