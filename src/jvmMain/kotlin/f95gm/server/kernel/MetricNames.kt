package f95gm.server.kernel

/** Stable metric label values shared by collectors and their HTTP renderers. */
internal object MetricNames {
    const val UNMATCHED_ENDPOINT = "unmatched"
    const val OTHER_ENDPOINT = "__other__"
    const val DEFAULT_NAMESPACE = "default"
    const val OTHER_NAMESPACE = "__other__"
}
