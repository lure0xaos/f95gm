package f95gm.server.config.logging

import f95gm.server.http.logging.redactLogValue

internal data class StructuredLogField(val name: String, val value: Any?)

internal sealed interface StructuredEvent {
    val name: String
    val fields: List<StructuredLogField>
}

internal enum class AuditEventName(val wireName: String) {
    LOGIN_FAILED("login_failed"),
    LOGIN_SUCCEEDED("login_succeeded"),
    LOGOUT("logout"),
    IDEMPOTENCY_REJECTED("idempotency_rejected"),
    SESSION_ROW_QUARANTINED("session_row_quarantined")
}

internal data class AuditEvent(
    val type: AuditEventName,
    override val fields: List<StructuredLogField>
) : StructuredEvent {
    override val name: String = "audit_${type.wireName}"
}

internal data class RequestCompletedEvent(
    val requestId: String,
    val method: String,
    val path: String,
    val status: Any?,
    val durationMs: Long,
    val outcome: String,
    val errorType: String?
) : StructuredEvent {
    override val name: String = "request_completed"
    override val fields: List<StructuredLogField> = listOf(
        logField("requestId", requestId), logField("method", method), logField("path", path),
        logField("status", status), logField("durationMs", durationMs), logField("outcome", outcome),
        logField("errorType", errorType)
    )
}

internal data class ServerStartedEvent(
    val address: Any?,
    val host: String,
    val port: Int,
    val startupMillis: Long
) : StructuredEvent {
    override val name: String = "server_started"
    override val fields: List<StructuredLogField> = listOf(
        logField("address", address),
        logField("host", host),
        logField("port", port),
        logField("startupMillis", startupMillis)
    )
}

internal data class ServerStoppedEvent(
    val uptimeMillis: Long,
    val activeRequests: Int,
    val requests: Long,
    val rejected: Long,
    val retries: Long,
    val cacheEvictions: Long,
    val circuitOpenings: Long
) : StructuredEvent {
    override val name: String = "server_stopped"
    override val fields: List<StructuredLogField> = listOf(
        logField("uptimeMillis", uptimeMillis), logField("activeRequests", activeRequests),
        logField("requests", requests), logField("rejected", rejected), logField("retries", retries),
        logField("cacheEvictions", cacheEvictions), logField("circuitOpenings", circuitOpenings)
    )
}

internal data class ConfigurationLoadedEvent(val values: Map<String, Any>) : StructuredEvent {
    override val name: String = "configuration_loaded"
    override val fields: List<StructuredLogField> = values.map { (key, value) -> logField(key, value) }
}

internal data class UpstreamRetryEvent(
    val method: String,
    val uri: String,
    val attempt: Int
) : StructuredEvent {
    override val name: String = "upstream_retry"
    override val fields: List<StructuredLogField> =
        listOf(logField("method", method), logField("uri", uri), logField("attempt", attempt))
}

internal data class UpstreamCircuitOpenEvent(val uri: String) : StructuredEvent {
    override val name: String = "upstream_circuit_open"
    override val fields: List<StructuredLogField> = listOf(logField("uri", uri))
}

internal data class UpstreamTransportFailureEvent(
    val method: String,
    val uri: String,
    val attempts: Int,
    val reason: String
) : StructuredEvent {
    override val name: String = "upstream_transport_failure"
    override val fields: List<StructuredLogField> = listOf(
        logField("method", method), logField("uri", uri), logField("attempts", attempts), logField("reason", reason)
    )
}

internal data class UpstreamResponseEvent(
    val method: String,
    val uri: String,
    val status: Int,
    val contentLength: String,
    val responseBytes: Int,
    val retryAttempts: Int,
    val durationMs: Long
) : StructuredEvent {
    override val name: String = "upstream_response"
    override val fields: List<StructuredLogField> = listOf(
        logField("method", method), logField("uri", uri), logField("status", status),
        logField("contentLength", contentLength), logField("responseBytes", responseBytes),
        logField("retryAttempts", retryAttempts), logField("durationMs", durationMs)
    )
}

internal fun logField(name: String, value: Any?): StructuredLogField = StructuredLogField(name, value)

internal fun structuredLog(event: String, fields: Iterable<StructuredLogField>): String = buildString {
    append("event=").append(escape(event))
    fields.filter { it.name in allowedFields[event].orEmpty() }.forEach { field ->
        append(' ').append(escape(field.name)).append('=')
            .append(escape(redactLogValue(field.value?.toString() ?: "-")))
    }
}

internal fun structuredLog(event: StructuredEvent): String = structuredLog(event.name, event.fields)

private val allowedFields = mapOf(
    "server_started" to setOf("address", "host", "port"),
    "server_stopped" to setOf(
        "uptimeMillis",
        "activeRequests",
        "requests",
        "rejected",
        "retries",
        "cacheEvictions",
        "circuitOpenings"
    ),
    "configuration_loaded" to setOf("host", "port", "database", "staticDirectory", "deploymentMode", "openBrowser"),
    "request_completed" to setOf("requestId", "method", "path", "status", "durationMs", "outcome", "errorType"),
    "upstream_circuit_open" to setOf("uri"),
    "upstream_response" to setOf(
        "method",
        "uri",
        "status",
        "contentLength",
        "responseBytes",
        "retryAttempts",
        "durationMs"
    ),
    "upstream_retry" to setOf("method", "uri", "attempt"),
    "upstream_transport_failure" to setOf("method", "uri", "attempts", "reason"),
    "audit_login_failed" to setOf("requestId", "account", "outcome"),
    "audit_login_succeeded" to setOf("requestId", "account", "outcome"),
    "audit_logout" to setOf("requestId", "account", "outcome"),
    "audit_idempotency_rejected" to setOf("requestId", "account", "reason", "outcome"),
    "audit_session_row_quarantined" to setOf("reason")
)

internal fun auditLog(event: AuditEvent): String = structuredLog(event)

private fun escape(value: String): String = buildString(value.length) {
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            ' ' -> append("\\s")
            '=' -> append("\\=")
            else -> append(character)
        }
    }
}
