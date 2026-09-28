package f95gm.server.observability

internal data class CacheEvictionEvent(val count: Int = 1) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.CACHE_EVICTION)
    override val fields: List<StructuredLogField> = listOf(logField(StructuredLogKeys.COUNT, count))
}
internal data class CacheLoadFailureEvent(
    val namespace: String,
    val category: String
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.CACHE_LOAD_FAILURE)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.NAMESPACE, namespace), logField(StructuredLogKeys.CATEGORY, category)
    )
}

internal data class SessionEvictionEvent(val count: Int) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.SESSION_EVICTION)
    override val fields: List<StructuredLogField> = listOf(logField(StructuredLogKeys.COUNT, count))
}

internal data class RequestCompletedEvent(
    val requestId: String,
    val method: String,
    val path: String,
    val status: String,
    val durationMs: Long,
    val outcome: RequestOutcome,
    val errorType: RequestErrorType?
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.REQUEST_COMPLETED)
    override val correlationId: String = requestId
    override val fields: List<StructuredLogField> = structuredFields {
        text(StructuredLogKeys.REQUEST_ID, requestId)
        text(StructuredLogKeys.METHOD, method)
        text(StructuredLogKeys.PATH, path)
        text(StructuredLogKeys.STATUS, status)
        integer(StructuredLogKeys.DURATION_MS, durationMs)
        text(StructuredLogKeys.OUTCOME, outcome.name.lowercase())
        text(StructuredLogKeys.ERROR_TYPE, errorType?.wireName)
    }
}

internal data class ServerStartedEvent(
    val address: String?,
    val host: String,
    val port: Int,
    val startupMillis: Long
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.SERVER_STARTED)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.ADDRESS, address),
        logField(StructuredLogKeys.HOST, host),
        logField(StructuredLogKeys.PORT, port),
        logField(StructuredLogKeys.STARTUP_MILLIS, startupMillis)
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
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.SERVER_STOPPED)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.UPTIME_MILLIS, uptimeMillis), logField(StructuredLogKeys.ACTIVE_REQUESTS, activeRequests),
        logField(StructuredLogKeys.REQUESTS, requests), logField(StructuredLogKeys.REJECTED, rejected), logField(StructuredLogKeys.RETRIES, retries),
        logField(StructuredLogKeys.CACHE_EVICTIONS, cacheEvictions), logField(StructuredLogKeys.CIRCUIT_OPENINGS, circuitOpenings)
    )
}

internal data class LifecycleMetricsEvent(
    val rollbackFailures: Long,
    val lifecycleRejections: Long,
    val configurationReloadRejections: Long
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.LIFECYCLE_METRICS)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.ROLLBACK_FAILURES, rollbackFailures),
        logField(StructuredLogKeys.LIFECYCLE_REJECTIONS, lifecycleRejections),
        logField(StructuredLogKeys.CONFIGURATION_RELOAD_REJECTIONS, configurationReloadRejections)
    )
}

internal data class ConfigurationLoadedEvent(
    val host: String,
    val port: Int,
    val database: String,
    val staticDirectoryConfigured: Boolean,
    val deploymentMode: String,
    val openBrowser: Boolean
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.CONFIGURATION_LOADED)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.HOST, host),
        logField(StructuredLogKeys.PORT, port),
        logField(StructuredLogKeys.DATABASE, database),
        logField(StructuredLogKeys.STATIC_DIRECTORY_CONFIGURED, staticDirectoryConfigured),
        logField(StructuredLogKeys.DEPLOYMENT_MODE, deploymentMode),
        logField(StructuredLogKeys.OPEN_BROWSER, openBrowser)
    )
}
