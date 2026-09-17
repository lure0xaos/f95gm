package f95gm.server.observability

import f95gm.server.model.http.ResponseBodyBytes
import f95gm.server.model.http.UpstreamFailure
import f95gm.server.kernel.RetryFailureReason
import kotlin.time.Duration

internal data class UpstreamRetryEvent(val method: String, val uri: String, val attempt: RetryAttempt) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.UPSTREAM_RETRY)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.METHOD, method), logField(StructuredLogKeys.URI, uri), logField(StructuredLogKeys.ATTEMPT, attempt)
    )
}

internal data class UpstreamRetryBudgetExhaustedEvent(
    val method: String,
    val uri: String,
    val reason: RetryFailureReason,
    val requestedDelay: Duration?,
    val remainingDelay: Duration?
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.UPSTREAM_RETRY_BUDGET_EXHAUSTED)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.METHOD, method), logField(StructuredLogKeys.URI, uri),
        logField(StructuredLogKeys.REASON, reason.name.lowercase()), logField(StructuredLogKeys.REQUESTED_DELAY_MS, requestedDelay),
        logField(StructuredLogKeys.REMAINING_DELAY_MS, remainingDelay)
    )
}

internal data class UpstreamCircuitOpenEvent(val uri: String) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.UPSTREAM_CIRCUIT_OPEN)
    override val fields: List<StructuredLogField> = listOf(logField(StructuredLogKeys.URI, uri))
}

internal data class UpstreamTransportFailureEvent(
    val method: String,
    val uri: String,
    val attempts: RetryAttempt,
    val reason: UpstreamFailure
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.UPSTREAM_TRANSPORT_FAILURE)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.METHOD, method), logField(StructuredLogKeys.URI, uri),
        logField(StructuredLogKeys.ATTEMPTS, attempts), logField(StructuredLogKeys.REASON, reason.name.lowercase())
    )
}

internal data class UpstreamResponseEvent(
    val method: String,
    val uri: String,
    val status: Int,
    val contentLength: String,
    val responseBytes: ResponseBodyBytes,
    val retryAttempts: RetryAttempt,
    val durationMs: RequestDurationMillis
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.UPSTREAM_RESPONSE)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.METHOD, method), logField(StructuredLogKeys.URI, uri), logField(StructuredLogKeys.STATUS, status),
        logField(StructuredLogKeys.CONTENT_LENGTH, contentLength), logField(StructuredLogKeys.RESPONSE_BYTES, responseBytes),
        logField(StructuredLogKeys.RETRY_ATTEMPTS, retryAttempts), logField(StructuredLogKeys.DURATION_MS, durationMs)
    )
}
