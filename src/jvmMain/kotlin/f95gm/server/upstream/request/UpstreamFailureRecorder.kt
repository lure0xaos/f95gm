package f95gm.server.upstream.request

import f95gm.server.kernel.FailureBoundary
import f95gm.server.observability.FailureReporter
import f95gm.server.observability.FailureScopes
import f95gm.server.observability.UpstreamFailureKey
import f95gm.server.observability.FailureDescriptor
import f95gm.server.observability.UpstreamMetricEndpoint
import f95gm.server.observability.UpstreamMetricOutcome
import f95gm.server.observability.UpstreamMetrics
import f95gm.server.observability.UpstreamMetricStatus
import f95gm.server.kernel.failureCategory
import f95gm.server.kernel.RetryExecutor
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.model.http.ResponseBodyBytes

/** Adapts terminal transport failures and records their circuit/metric effects. */
internal class UpstreamFailureRecorder(
    private val circuitBreaker: UpstreamCircuitBreaker,
    private val metrics: UpstreamMetrics
) {
    fun transportFailure(
        key: UpstreamCircuitKey,
        timing: UpstreamRequestTiming,
        error: Throwable
    ): UpstreamResponse {
        val outcome = requireNotNull(UpstreamTransportFailureAdapter.adapt(error))
        val status = if (error is UpstreamRequestDeadlineExceeded) 504 else 502
        return recordFailure(key, timing, error, outcome, status)
    }

    fun retryBudgetFailure(
        key: UpstreamCircuitKey,
        timing: UpstreamRequestTiming,
        deadlineBound: Boolean,
        error: RetryExecutor
    ): UpstreamResponse {
        val outcome = if (deadlineBound) UpstreamFailurePolicy.timeout() else UpstreamFailurePolicy.transport()
        return recordFailure(key, timing, error, outcome, if (deadlineBound) 504 else 502)
    }

    fun retryFailure(
        key: UpstreamCircuitKey,
        timing: UpstreamRequestTiming,
        failure: RetryOutcome.Failure
    ): UpstreamResponse = recordFailure(
        key,
        timing,
        null,
        UpstreamFailurePolicy.retryFailure(failure),
        502
    )

    private fun recordFailure(
        key: UpstreamCircuitKey,
        timing: UpstreamRequestTiming,
        error: Throwable?,
        outcome: UpstreamResponseDisposition,
        status: Int
    ): UpstreamResponse {
        error?.let {
            FailureReporter.report(
                FailureScopes.upstream(UpstreamFailureKey(key.value)),
                FailureDescriptor(FailureBoundary.UPSTREAM, it.failureCategory()),
                it
            )
        }
        applyCircuitAction(key, outcome.circuitAction)
        metrics.record(
            UpstreamMetricEndpoint(key.value), UpstreamMetricStatus(status), outcome.retryAttempts,
            ResponseBodyBytes(0), timing.elapsed(), outcome.metricOutcome, outcome.retryReason
        )
        return outcome.response
    }

    fun applyCircuitAction(key: UpstreamCircuitKey, action: UpstreamCircuitAction) {
        when (action) {
            UpstreamCircuitAction.NONE -> Unit
            UpstreamCircuitAction.RECORD_FAILURE -> circuitBreaker.recordFailure(key)
            UpstreamCircuitAction.RECORD_SUCCESS -> circuitBreaker.recordSuccess(key)
        }
    }
}
