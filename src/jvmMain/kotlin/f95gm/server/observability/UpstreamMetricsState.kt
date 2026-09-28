package f95gm.server.observability

import f95gm.server.model.http.UpstreamRetryReason

internal data class UpstreamMetricsState(
    val requests: Long,
    val responseBytes: Long,
    val retryAttempts: Long,
    val totalDurationMillis: Long,
    val retryBudgetExhaustions: Long,
    val retryDeadlineExhaustions: Long,
    val retryDeadlineCancellations: Long,
    val retryCallerCancellations: Long,
    val byEndpointAndStatus: Map<UpstreamMetricDimension, Long>,
    val byOutcome: Map<UpstreamMetricOutcome, Long>,
    val byRetryReason: Map<UpstreamRetryReason, Long>,
    val countersConsistent: Boolean = true
)

internal data class UpstreamMetricDimension(
    val endpoint: UpstreamMetricEndpoint,
    val status: UpstreamMetricStatus
)
