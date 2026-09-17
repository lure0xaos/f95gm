package f95gm.server.app

import f95gm.server.kernel.MetricNames

internal enum class RequestMetricEndpoint(val wireName: String) {
    UNMATCHED(MetricNames.UNMATCHED_ENDPOINT)
}
