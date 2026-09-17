package f95gm.server.observability

/** Feature ownership markers keep audit, lifecycle, and upstream log contracts discoverable. */
internal object AuditLogSchema {
    val eventNames = AuditEventName.entries.map { "audit_${it.wireName}" }.toSet()
}

internal object LifecycleLogSchema {
    val eventNames = setOf(
        "server_started", "server_stopped", "configuration_loaded", "configuration_reloaded",
        "configuration_reload_unchanged", "lifecycle_metrics", "lifecycle_state_changed",
        "database_backup_created", "database_migration_failed"
    )
}

internal object UpstreamLogSchema {
    val eventNames = setOf(
        "upstream_retry", "upstream_retry_budget_exhausted", "upstream_circuit_open",
        "upstream_transport_failure", "upstream_response"
    )
}
