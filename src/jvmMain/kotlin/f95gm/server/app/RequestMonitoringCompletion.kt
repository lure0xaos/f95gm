package f95gm.server.app

import f95gm.server.observability.RequestCompletedEvent
import f95gm.server.observability.RequestErrorType
import f95gm.server.observability.RequestOutcome
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog
import f95gm.server.http.request.RequestEndpoint
import f95gm.server.http.request.RequestMetrics
import f95gm.server.http.request.RequestId
import f95gm.server.http.request.requestPath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.*
import f95gm.server.kernel.ClockSnapshot
import kotlin.time.Duration.Companion.nanoseconds

internal fun recordRequestCompletion(
    call: ApplicationCall,
    requestId: RequestId,
    started: ClockSnapshot,
    endpoint: RequestEndpoint,
    metrics: RequestMetrics,
    failure: RequestFailure?,
    now: () -> ClockSnapshot
) {
    val duration = (now().monotonicNanos.value - started.monotonicNanos.value).coerceAtLeast(0L).nanoseconds
    metrics.record(duration, endpoint)
    logger.info {
        structuredLog(
            RequestCompletedEvent(
                requestId = requestId.value,
                method = call.request.httpMethod.value,
                path = call.request.requestPath().value,
                status = call.response.status()?.value?.toString() ?: "-",
                durationMs = duration.inWholeMilliseconds,
                outcome = if (failure == null) RequestOutcome.SUCCESS else RequestOutcome.ERROR,
                errorType = failure?.kind?.let {
                    when (it) {
                        RequestFailureKind.API_FAILURE -> RequestErrorType.API_FAILURE
                        RequestFailureKind.REQUEST_FAILED, RequestFailureKind.CANCELLED -> RequestErrorType.REQUEST_FAILED
                    }
                }
            )
        )
    }
}
