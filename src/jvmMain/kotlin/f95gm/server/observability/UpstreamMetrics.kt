package f95gm.server.observability

import java.util.concurrent.ConcurrentHashMap
import f95gm.domain.api.UpstreamMetricsSnapshotContract
import f95gm.server.model.http.UpstreamRetryReason
import f95gm.server.model.http.ResponseBodyBytes
import f95gm.server.kernel.MetricCounter
import f95gm.server.kernel.BoundedCardinality
import f95gm.server.kernel.ConsistentSnapshotSequence
import kotlin.time.Duration

private data class UpstreamMetricKey(val endpoint: UpstreamMetricEndpoint, val status: UpstreamMetricStatus)

internal class UpstreamMetrics(private val maxEndpointCardinality: Int = DEFAULT_MAX_ENDPOINTS) {
    companion object {
        const val DEFAULT_MAX_ENDPOINTS = DEFAULT_UPSTREAM_METRIC_ENDPOINTS
        const val OTHER_ENDPOINT = OTHER_UPSTREAM_METRIC_ENDPOINT
        private const val SNAPSHOT_ATTEMPTS = 3
    }

    init {
        require(maxEndpointCardinality > 0)
    }

    private val requests = MetricCounter()
    private val responseBytes = MetricCounter()
    private val retryAttempts = MetricCounter()
    private val totalDurationMillis = MetricCounter()
    private val byEndpointAndStatus = ConcurrentHashMap<UpstreamMetricKey, MetricCounter>()
    private val byOutcome = ConcurrentHashMap<UpstreamMetricOutcome, MetricCounter>()
    private val byRetryReason = ConcurrentHashMap<UpstreamRetryReason, MetricCounter>()
    private val retryBudgetExhaustions = MetricCounter()
    private val retryDeadlineExhaustions = MetricCounter()
    private val retryDeadlineCancellations = MetricCounter()
    private val retryCallerCancellations = MetricCounter()
    private val endpointCardinality = BoundedCardinality<UpstreamMetricEndpoint>(
        (maxEndpointCardinality - 1).coerceAtLeast(0)
    )
    private val updateSequence = ConsistentSnapshotSequence()

    fun record(
        endpoint: UpstreamMetricEndpoint,
        status: UpstreamMetricStatus,
        retries: Int,
        bytes: ResponseBodyBytes,
        duration: Duration,
        outcome: UpstreamMetricOutcome,
        retryReason: UpstreamRetryReason? = null
    ) {
        updateSequence.withUpdate {
            requests.increment()
            responseBytes.add(bytes.value)
            retryAttempts.add(retries.toLong().coerceAtLeast(0L))
            totalDurationMillis.add(duration.inWholeMilliseconds.coerceAtLeast(0L))
            byEndpointAndStatus.computeIfAbsent(UpstreamMetricKey(endpointFor(endpoint), status.bucket)) { MetricCounter() }.increment()
            byOutcome.computeIfAbsent(outcome) { MetricCounter() }.increment()
            retryReason?.let { byRetryReason.computeIfAbsent(it) { MetricCounter() }.increment() }
        }
    }

    fun snapshot(): UpstreamMetricsSnapshotContract {
        val snapshot = updateSequence.read(SNAPSHOT_ATTEMPTS, ::metricsState)
        return snapshot.value.copy(countersConsistent = snapshot.consistent).toContract()
    }

    fun recordRetryBudgetExhaustion(deadlineBound: Boolean) {
        updateSequence.withUpdate {
            retryBudgetExhaustions.increment()
            if (deadlineBound) retryDeadlineExhaustions.increment()
        }
    }

    fun recordRetryDeadlineCancellation() = retryDeadlineCancellations.increment()

    fun recordRetryCallerCancellation() = retryCallerCancellations.increment()

    private fun metricsState(): UpstreamMetricsState = UpstreamMetricsState(
        requests = requests.get(),
        responseBytes = responseBytes.get(),
        retryAttempts = retryAttempts.get(),
        totalDurationMillis = totalDurationMillis.get(),
        retryBudgetExhaustions = retryBudgetExhaustions.get(),
        retryDeadlineExhaustions = retryDeadlineExhaustions.get(),
        retryDeadlineCancellations = retryDeadlineCancellations.get(),
        retryCallerCancellations = retryCallerCancellations.get(),
        byEndpointAndStatus = byEndpointAndStatus.entries.associate {
            UpstreamMetricDimension(it.key.endpoint, it.key.status) to it.value.get()
        },
        byOutcome = byOutcome.entries.associate { it.key to it.value.get() },
        byRetryReason = byRetryReason.entries.associate { it.key to it.value.get() }
    )

    private fun endpointFor(endpoint: UpstreamMetricEndpoint): UpstreamMetricEndpoint {
        return if (endpoint.value == OTHER_ENDPOINT || endpointCardinality.admit(endpoint)) {
            endpoint
        } else {
            UpstreamMetricEndpoint(OTHER_ENDPOINT)
        }
    }

}

