package f95gm.server.upstream.request

import f95gm.server.observability.UpstreamMetricOutcome

import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.model.http.UpstreamResponse
import io.ktor.http.HttpStatusCode

/** Converts transport-level failures into one response/metrics/circuit decision. */
internal object UpstreamFailurePolicy {
    fun transport(): UpstreamResponseDisposition = UpstreamOutcomePolicy.special(
        transportFailureResponse(),
        UpstreamMetricOutcome.TRANSPORT_FAILURE,
        UpstreamCircuitAction.RECORD_FAILURE
    )

    fun timeout(): UpstreamResponseDisposition = UpstreamOutcomePolicy.special(
        timeoutFailureResponse(),
        UpstreamMetricOutcome.TIMEOUT,
        UpstreamCircuitAction.RECORD_FAILURE
    )

    fun retryFailure(failure: RetryOutcome.Failure): UpstreamResponseDisposition {
        val response = UpstreamResponse.Failure(
            status = HttpStatusCatalog.BAD_GATEWAY,
            body = "",
            contentType = null,
            reason = failure.failure
        )
        return UpstreamOutcomePolicy.decide(response, HttpStatusCatalog.BAD_GATEWAY, failure.attempts, failure.reason)
    }
}
