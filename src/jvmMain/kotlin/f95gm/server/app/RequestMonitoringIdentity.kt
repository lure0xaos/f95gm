package f95gm.server.app

import f95gm.server.http.request.RequestId
import io.ktor.server.application.ApplicationCall

internal fun RequestMonitoringConfiguration.requestId(call: ApplicationCall): RequestId =
    call.requestIdHeader()?.let(RequestId::parseOrNull) ?: requestIdGenerator.next()
