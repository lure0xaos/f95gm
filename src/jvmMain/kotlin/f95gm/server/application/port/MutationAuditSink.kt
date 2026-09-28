package f95gm.server.application.port

internal enum class MutationAuditKind { MARK, FILTER }

internal enum class MutationAuditOperation { STORE, IMPORT, REMOVE }
internal enum class MutationAuditResult { STORED, CONFLICT, REMOVED }

internal sealed interface MutationAuditOutcome {
    data class Completed(val result: MutationAuditResult) : MutationAuditOutcome
}

internal data class MutationAuditEvent(
    val kind: MutationAuditKind,
    val account: String,
    val operation: MutationAuditOperation,
    val outcome: MutationAuditOutcome
)

/** Application-facing audit seam; concrete logging stays outside application services. */
internal fun interface MutationAuditSink {
    fun record(event: MutationAuditEvent)
}

internal object NoopMutationAuditSink : MutationAuditSink {
    override fun record(event: MutationAuditEvent) = Unit
}
