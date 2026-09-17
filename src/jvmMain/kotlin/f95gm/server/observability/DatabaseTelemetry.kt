package f95gm.server.observability

import f95gm.server.kernel.MonotonicNanos
import f95gm.server.kernel.ServerClock

internal enum class DatabaseOperation(val wireName: String) {
    TRANSACTION("transaction"),
    SESSIONS_RESTORE("sessions.restore"),
    SESSIONS_REMOVE("sessions.remove"),
    SESSIONS_PERSIST("sessions.persist"),
    MARKS_LOAD("marks.load"),
    MARKS_STORE("marks.store"),
    MARKS_STORE_IF_CURRENT("marks.store_if_current"),
    MARKS_REMOVE("marks.remove"),
    FILTERS_LOAD("filters.load"),
    FILTERS_STORE("filters.store"),
    FILTERS_STORE_ALL("filters.store_all"),
    FILTERS_STORE_IF_CURRENT("filters.store_if_current"),
    FILTERS_REMOVE("filters.remove");

    companion object {
        val wireNames: Set<String> = entries.map(DatabaseOperation::wireName).toSet()
    }
}

internal enum class DatabaseOperationOutcome { SUCCESS, FAILURE }

internal data class DatabaseOperationEvent(
    val operation: DatabaseOperation,
    val durationMillis: Long,
    val attempts: Int,
    val outcome: DatabaseOperationOutcome,
    val errorType: String?
) : StructuredEvent {
    override val name = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.DATABASE_OPERATION)
    override val fields = listOf(
        logField(StructuredLogKeys.OPERATION, operation.wireName),
        logField(StructuredLogKeys.DURATION_MS, durationMillis),
        logField(StructuredLogKeys.RETRY_ATTEMPTS, (attempts - 1).coerceAtLeast(0)),
        logField(StructuredLogKeys.OUTCOME, outcome.name.lowercase()),
        logField(StructuredLogKeys.ERROR_TYPE, errorType)
    )
}

internal fun interface DatabaseOperationTelemetry {
    fun record(event: DatabaseOperationEvent)

    companion object {
        val logging: DatabaseOperationTelemetry = DatabaseOperationTelemetry { event ->
            logger.debug { structuredLog(event) }
        }
        val noop: DatabaseOperationTelemetry = DatabaseOperationTelemetry { }
    }
}

internal fun databaseDurationMillis(clock: ServerClock, startedAt: MonotonicNanos): Long =
    clock.elapsedSince(startedAt).inWholeMilliseconds.coerceAtLeast(0L)
