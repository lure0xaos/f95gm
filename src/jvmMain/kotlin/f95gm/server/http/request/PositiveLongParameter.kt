package f95gm.server.http.request

import f95gm.domain.values.F95ThreadId
import f95gm.domain.api.parsePositiveLong
import io.ktor.server.application.*

internal fun ApplicationCall.positiveLongParameter(name: String): String? =
    parameters[name]?.parsePositiveLong()?.toString()

internal fun ApplicationCall.positiveThreadIdParameter(name: String): F95ThreadId? =
    positiveLongParameter(name)?.let(::F95ThreadId)
