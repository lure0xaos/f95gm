package f95gm.server.upstream.request

import f95gm.server.observability.UpstreamMetricOutcome

import f95gm.server.model.http.UpstreamResponse
import f95gm.server.model.http.UpstreamRetryReason
import io.ktor.http.HttpStatusCode

internal enum class UpstreamCircuitAction { NONE, RECORD_SUCCESS, RECORD_FAILURE }

internal data class UpstreamResponseDisposition(
    val response: UpstreamResponse,
    val retryAttempts: Int,
    val retryReason: UpstreamRetryReason?,
    val metricOutcome: UpstreamMetricOutcome,
    val circuitAction: UpstreamCircuitAction
)

internal object UpstreamOutcomePolicy {
    fun special(
        response: UpstreamResponse,
        metricOutcome: UpstreamMetricOutcome,
        circuitAction: UpstreamCircuitAction = UpstreamCircuitAction.NONE
    ) = UpstreamResponseDisposition(
        response = response,
        retryAttempts = response.retryAttempts,
        retryReason = response.retryReason,
        metricOutcome = metricOutcome,
        circuitAction = circuitAction
    )

    fun decide(
        response: UpstreamResponse,
        upstreamStatus: HttpStatusCode,
        retryAttempts: Int,
        retryReason: UpstreamRetryReason?
    ): UpstreamResponseDisposition {
        val failure = response as? UpstreamResponse.Failure
        val tripsCircuit = failure?.reason?.tripsCircuit == true ||
            failure == null && upstreamStatus in retryableStatuses
        return UpstreamResponseDisposition(
            response = response,
            retryAttempts = retryAttempts,
            retryReason = retryReason,
            metricOutcome = when (response) {
                is UpstreamResponse.Success -> UpstreamMetricOutcome.SUCCESS
                is UpstreamResponse.Failure -> UpstreamMetricOutcome.UPSTREAM_FAILURE
            },
            circuitAction = if (tripsCircuit) UpstreamCircuitAction.RECORD_FAILURE else UpstreamCircuitAction.RECORD_SUCCESS
        )
    }
}
