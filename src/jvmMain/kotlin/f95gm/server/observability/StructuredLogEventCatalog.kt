package f95gm.server.observability

/** Declarative wire-level catalog for structured log events and their bounded fields. */
internal object StructuredLogEventCatalog {
    enum class Field(val wireName: String) {
        ADDRESS("address"), HOST("host"), PORT("port"), UPTIME_MILLIS("uptimeMillis"), ACTIVE_REQUESTS("activeRequests"),
        REQUESTS("requests"), REJECTED("rejected"), RETRIES("retries"), CACHE_EVICTIONS("cacheEvictions"),
        CIRCUIT_OPENINGS("circuitOpenings"), DATABASE("database"), STATIC_DIRECTORY("staticDirectory"),
        DEPLOYMENT_MODE("deploymentMode"), OPEN_BROWSER("openBrowser"), REQUEST_ID("requestId"), METHOD("method"),
        PATH("path"), STATUS("status"), DURATION_MS("durationMs"), OUTCOME("outcome"), ERROR_TYPE("errorType"),
        URI("uri"), CONTENT_LENGTH("contentLength"), RESPONSE_BYTES("responseBytes"), RETRY_ATTEMPTS("retryAttempts"),
        ATTEMPT("attempt"), ATTEMPTS("attempts"), REASON("reason"), REQUESTED_DELAY_MS("requestedDelayMs"),
        REMAINING_DELAY_MS("remainingDelayMs"), IMPACT("impact"), RESTART_REASONS("restartReasons"), SCOPE("scope"),
        EXCEPTION_TYPE("exceptionType"), ACCOUNT("account"), OPERATION("operation"), BACKUP("backup"), COUNT("count"),
        NAMESPACE("namespace"), CATEGORY("category"), STARTUP_MILLIS("startupMillis"),
        STATIC_DIRECTORY_CONFIGURED("staticDirectoryConfigured"), CODE("code"), MESSAGE_KEY("messageKey"),
        RESOURCE_TYPE("resourceType"), STATE("state"), ROLLBACK_FAILURES("rollbackFailures"),
        LIFECYCLE_REJECTIONS("lifecycleRejections"), CONFIGURATION_RELOAD_REJECTIONS("configurationReloadRejections")
    }

    internal enum class Event(val wireName: String, vararg fields: Field) {
        SERVER_STARTED("server_started", Field.ADDRESS, Field.HOST, Field.PORT, Field.STARTUP_MILLIS),
        SERVER_STOPPED("server_stopped", Field.UPTIME_MILLIS, Field.ACTIVE_REQUESTS, Field.REQUESTS, Field.REJECTED, Field.RETRIES, Field.CACHE_EVICTIONS, Field.CIRCUIT_OPENINGS),
        CONFIGURATION_LOADED("configuration_loaded", Field.HOST, Field.PORT, Field.DATABASE, Field.STATIC_DIRECTORY_CONFIGURED, Field.DEPLOYMENT_MODE, Field.OPEN_BROWSER),
        CONFIGURATION_RELOADED("configuration_reloaded", Field.IMPACT, Field.RESTART_REASONS),
        CONFIGURATION_RELOAD_UNCHANGED("configuration_reload_unchanged", Field.REASON),
        FAILURE_REPORTED("failure_reported", Field.SCOPE, Field.CATEGORY, Field.EXCEPTION_TYPE),
        REQUEST_COMPLETED("request_completed", Field.REQUEST_ID, Field.METHOD, Field.PATH, Field.STATUS, Field.DURATION_MS, Field.OUTCOME, Field.ERROR_TYPE),
        LIFECYCLE_METRICS("lifecycle_metrics", Field.ROLLBACK_FAILURES, Field.LIFECYCLE_REJECTIONS, Field.CONFIGURATION_RELOAD_REJECTIONS),
        LIFECYCLE_STATE_CHANGED("lifecycle_state_changed", Field.RESOURCE_TYPE, Field.STATE),
        UPSTREAM_CIRCUIT_OPEN("upstream_circuit_open", Field.URI),
        UPSTREAM_RESPONSE("upstream_response", Field.METHOD, Field.URI, Field.STATUS, Field.CONTENT_LENGTH, Field.RESPONSE_BYTES, Field.RETRY_ATTEMPTS, Field.DURATION_MS),
        UPSTREAM_RETRY("upstream_retry", Field.METHOD, Field.URI, Field.ATTEMPT),
        UPSTREAM_RETRY_BUDGET_EXHAUSTED("upstream_retry_budget_exhausted", Field.METHOD, Field.URI, Field.REASON, Field.REQUESTED_DELAY_MS, Field.REMAINING_DELAY_MS),
        UPSTREAM_TRANSPORT_FAILURE("upstream_transport_failure", Field.METHOD, Field.URI, Field.ATTEMPTS, Field.REASON),
        AUDIT_LOGIN_FAILED("audit_login_failed", Field.REQUEST_ID, Field.ACCOUNT, Field.OUTCOME),
        AUDIT_LOGIN_SUCCEEDED("audit_login_succeeded", Field.REQUEST_ID, Field.ACCOUNT, Field.OUTCOME),
        AUDIT_AUTHORIZATION_REJECTED("audit_authorization_rejected", Field.REQUEST_ID, Field.REASON, Field.OUTCOME),
        AUDIT_LOGOUT("audit_logout", Field.REQUEST_ID, Field.ACCOUNT, Field.OUTCOME),
        AUDIT_IDEMPOTENCY_REJECTED("audit_idempotency_rejected", Field.REQUEST_ID, Field.ACCOUNT, Field.REASON, Field.OUTCOME),
        AUDIT_SESSION_ROW_QUARANTINED("audit_session_row_quarantined", Field.REASON),
        AUDIT_ORIGIN_REJECTED("audit_origin_rejected", Field.REASON),
        AUDIT_CSRF_REJECTED("audit_csrf_rejected", Field.REQUEST_ID, Field.REASON),
        AUDIT_RATE_LIMIT_REJECTED("audit_rate_limit_rejected", Field.REASON),
        AUDIT_MARK_MUTATION("audit_mark_mutation", Field.REQUEST_ID, Field.ACCOUNT, Field.OPERATION, Field.OUTCOME),
        AUDIT_FILTER_MUTATION("audit_filter_mutation", Field.REQUEST_ID, Field.ACCOUNT, Field.OPERATION, Field.OUTCOME),
        DATABASE_BACKUP_CREATED("database_backup_created", Field.PATH),
        DATABASE_OPERATION("database_operation", Field.OPERATION, Field.DURATION_MS, Field.RETRY_ATTEMPTS, Field.OUTCOME, Field.ERROR_TYPE),
        DATABASE_MIGRATION_FAILED("database_migration_failed", Field.BACKUP),
        CACHE_EVICTION("cache_eviction", Field.COUNT),
        CACHE_LOAD_FAILURE("cache_load_failure", Field.NAMESPACE, Field.CATEGORY),
        SESSION_EVICTION("session_eviction", Field.COUNT),
        API_FAILURE("api_failure", Field.STATUS, Field.CODE, Field.MESSAGE_KEY, Field.EXCEPTION_TYPE),
        RESOURCE_CLOSE_FAILURE("resource_close_failure", Field.RESOURCE_TYPE, Field.EXCEPTION_TYPE);

        val fields: Set<Field> = fields.toSet()
    }

    val events: Map<StructuredEventName, Set<Field>> = Event.entries.associate {
        StructuredEventName(it.wireName) to it.fields
    }

    private val eventValueKinds: Map<StructuredEventName, Map<Field, Set<StructuredLogValueKind>>> = mapOf(
        StructuredEventName(Event.REQUEST_COMPLETED.wireName) to mapOf(Field.STATUS to setOf(StructuredLogValueKind.TEXT, StructuredLogValueKind.INTEGER)),
        StructuredEventName(Event.UPSTREAM_RESPONSE.wireName) to mapOf(Field.STATUS to setOf(StructuredLogValueKind.INTEGER)),
        StructuredEventName(Event.API_FAILURE.wireName) to mapOf(Field.STATUS to setOf(StructuredLogValueKind.INTEGER))
    )

    fun eventName(event: Event): StructuredEventName = StructuredEventName(event.wireName)

    fun allowedValueKinds(event: StructuredEventName, field: StructuredFieldName): Set<StructuredLogValueKind> {
        return eventValueKinds[event]?.entries
            ?.firstOrNull { it.key.wireName == field.value }
            ?.value
            ?: allowedValueKinds(field)
    }

    fun allowedValueKinds(field: StructuredFieldName): Set<StructuredLogValueKind> = when (field.value) {
        "port", "uptimeMillis", "activeRequests", "requests", "rejected", "retries", "cacheEvictions",
        "circuitOpenings", "durationMs", "responseBytes", "retryAttempts", "attempt",
        "attempts", "requestedDelayMs", "remainingDelayMs", "rollbackFailures", "lifecycleRejections",
        "configurationReloadRejections", "count", "startupMillis" -> setOf(StructuredLogValueKind.INTEGER)
        "openBrowser", "staticDirectoryConfigured" -> setOf(StructuredLogValueKind.BOOLEAN)
        "status" -> setOf(StructuredLogValueKind.TEXT, StructuredLogValueKind.INTEGER)
        else -> setOf(StructuredLogValueKind.TEXT)
    }
}
