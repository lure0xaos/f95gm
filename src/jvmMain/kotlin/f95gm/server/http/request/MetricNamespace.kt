package f95gm.server.http.request

/** Typed observability namespace; cache implementation identifiers stay behind the metrics adapter. */
@JvmInline
internal value class MetricNamespace(val value: String) {
    init { require(value.isNotBlank()) }
}
