package f95gm.server.observability

import f95gm.domain.api.UpstreamMetricsSnapshotContract
import f95gm.domain.api.UpstreamOutcomeCounts
import f95gm.domain.api.UpstreamRetryReasonCounts
import f95gm.server.model.http.UpstreamRetryReason

internal fun UpstreamMetricsState.toContract(): UpstreamMetricsSnapshotContract = UpstreamMetricsSnapshotContract(
    requests = requests,
    responseBytes = responseBytes,
    retryAttempts = retryAttempts,
    totalDurationMillis = totalDurationMillis,
    retryBudgetExhaustions = retryBudgetExhaustions,
    retryDeadlineExhaustions = retryDeadlineExhaustions,
    retryDeadlineCancellations = retryDeadlineCancellations,
    retryCallerCancellations = retryCallerCancellations,
    countersConsistent = countersConsistent,
    byEndpointAndStatus = byEndpointAndStatus.entries
        .sortedBy { "${it.key.endpoint.value}:${it.key.status.value}" }
        .associate { "${it.key.endpoint.value}:${it.key.status.value}" to it.value },
    outcomes = UpstreamOutcomeCounts(
        success = byOutcome[UpstreamMetricOutcome.SUCCESS] ?: 0L,
        upstreamFailure = byOutcome[UpstreamMetricOutcome.UPSTREAM_FAILURE] ?: 0L,
        transportFailure = byOutcome[UpstreamMetricOutcome.TRANSPORT_FAILURE] ?: 0L,
        timeout = byOutcome[UpstreamMetricOutcome.TIMEOUT] ?: 0L,
        circuitOpen = byOutcome[UpstreamMetricOutcome.CIRCUIT_OPEN] ?: 0L
    ),
    retryReasons = UpstreamRetryReasonCounts(
        httpStatus = byRetryReason[UpstreamRetryReason.HTTP_STATUS] ?: 0L,
        ioException = byRetryReason[UpstreamRetryReason.IO_EXCEPTION] ?: 0L
    )
)
