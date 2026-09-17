package f95gm.server.observability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import f95gm.server.model.http.UpstreamRetryReason
import f95gm.server.model.http.ResponseBodyBytes
import kotlin.time.Duration.Companion.milliseconds

class UpstreamMetricsTest {
    @Test
    fun dynamicEndpointMetricsAreBounded() {
        val metrics = UpstreamMetrics(maxEndpointCardinality = 2)
        repeat(10) { index ->
            metrics.record(
                UpstreamMetricEndpoint("/dynamic/$index"),
                UpstreamMetricStatus(200),
                retries = 0,
                bytes = ResponseBodyBytes(1),
                duration = 1.milliseconds,
                outcome = UpstreamMetricOutcome.SUCCESS,
                retryReason = if (index == 0) UpstreamRetryReason.HTTP_STATUS else null
            )
        }

        val dimensions = metrics.snapshot().byEndpointAndStatus
        assertEquals(2, dimensions.size)
        assertEquals(9, dimensions["__other__:200"])
    }

    @Test
    fun endpointLimitMustBePositive() {
        assertFailsWith<IllegalArgumentException> { UpstreamMetrics(0) }
    }
}
