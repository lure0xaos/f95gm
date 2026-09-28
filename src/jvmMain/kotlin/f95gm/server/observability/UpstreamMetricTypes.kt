package f95gm.server.observability

import f95gm.server.kernel.MetricNames

@JvmInline
internal value class UpstreamMetricEndpoint(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
internal value class UpstreamMetricStatus(val value: Int) {
    init { require(value in 100..599) }
    val code: Int get() = value
    val bucket: UpstreamMetricStatus get() = UpstreamMetricStatus((value / 100) * 100)
}

internal enum class UpstreamMetricOutcome(val key: String) {
    SUCCESS("success"), UPSTREAM_FAILURE("upstream_failure"), TRANSPORT_FAILURE("transport_failure"),
    TIMEOUT("timeout"), CIRCUIT_OPEN("circuit_open")
}

internal const val DEFAULT_UPSTREAM_METRIC_ENDPOINTS = 64
internal const val OTHER_UPSTREAM_METRIC_ENDPOINT = MetricNames.OTHER_ENDPOINT
