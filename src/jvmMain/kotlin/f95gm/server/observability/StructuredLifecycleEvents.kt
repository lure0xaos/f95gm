package f95gm.server.observability

internal enum class AuditEventName(val wireName: String) {
    LOGIN_FAILED("login_failed"),
    LOGIN_SUCCEEDED("login_succeeded"),
    AUTHORIZATION_REJECTED("authorization_rejected"),
    LOGOUT("logout"),
    IDEMPOTENCY_REJECTED("idempotency_rejected"),
    SESSION_ROW_QUARANTINED("session_row_quarantined"),
    ORIGIN_REJECTED("origin_rejected"),
    CSRF_REJECTED("csrf_rejected"),
    RATE_LIMIT_REJECTED("rate_limit_rejected"),
    MARK_MUTATION("mark_mutation"),
    FILTER_MUTATION("filter_mutation")
}

internal data class AuditEvent(
    val type: AuditEventName,
    override val fields: List<StructuredLogField>
) : StructuredEvent {
    override val name: StructuredEventName = StructuredEventName("audit_${type.wireName}")
}

internal enum class SessionLifecycleEventName(val wireName: String) {
    PERSISTED("session_persisted"), REMOVED("session_removed"), RESTORED("sessions_restored")
}

internal data class SessionLifecycleEvent(
    val type: SessionLifecycleEventName,
    override val fields: List<StructuredLogField> = emptyList()
) : StructuredEvent {
    override val name: StructuredEventName = StructuredEventName(type.wireName)
}

internal enum class DatabaseLifecycleEventName(val wireName: String) {
    BACKUP_CREATED("database_backup_created"),
    MIGRATION_FAILED("database_migration_failed")
}

internal data class DatabaseLifecycleEvent(
    val type: DatabaseLifecycleEventName,
    override val fields: List<StructuredLogField> = emptyList()
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(
        when (type) {
            DatabaseLifecycleEventName.BACKUP_CREATED -> StructuredLogEventCatalog.Event.DATABASE_BACKUP_CREATED
            DatabaseLifecycleEventName.MIGRATION_FAILED -> StructuredLogEventCatalog.Event.DATABASE_MIGRATION_FAILED
        }
    )
}

internal data class LifecycleStateChangedEvent(
    val resource: f95gm.server.kernel.LifecycleResourceName,
    val state: f95gm.server.kernel.LifecycleState
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.LIFECYCLE_STATE_CHANGED)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.RESOURCE_TYPE, resource.value),
        logField(StructuredLogKeys.STATE, state.wireName)
    )
}
