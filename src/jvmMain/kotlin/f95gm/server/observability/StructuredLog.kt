package f95gm.server.observability

import f95gm.server.observability.*

internal enum class RequestOutcome { SUCCESS, ERROR }

internal enum class AuditOutcome(val wireName: String) { SUCCESS("success"), FAILURE("failure") }

internal enum class RequestErrorType(val wireName: String) {
    REQUEST_FAILED("request_failed"),
    API_FAILURE("api_failure")
}

internal object StructuredLogSinks {
    val text: StructuredLogSink = StructuredLogSink { event ->
        structuredLogText(event.name, event.fields)
    }
}

/** The event registry is the stable schema boundary for operational consumers. */
internal object StructuredLogEventRegistry {
    val names: Set<String> = StructuredLogSchema.names
}
