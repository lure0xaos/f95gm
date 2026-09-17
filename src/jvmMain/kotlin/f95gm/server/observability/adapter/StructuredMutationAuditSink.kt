package f95gm.server.observability.adapter

import f95gm.server.application.port.MutationAuditKind
import f95gm.server.application.port.MutationAuditEvent
import f95gm.server.application.port.MutationAuditSink
import f95gm.server.application.port.MutationAuditResult
import f95gm.server.observability.AuditEvent
import f95gm.server.observability.AuditEventName
import f95gm.server.observability.auditLog
import f95gm.server.observability.logField
import f95gm.server.observability.logger
import f95gm.server.observability.StructuredLogKeys
import f95gm.server.application.port.MutationAuditOutcome

internal object StructuredMutationAuditSink : MutationAuditSink {
    override fun record(event: MutationAuditEvent) {
        val eventName = when (event.kind) {
            MutationAuditKind.MARK -> AuditEventName.MARK_MUTATION
            MutationAuditKind.FILTER -> AuditEventName.FILTER_MUTATION
        }
        logger.info {
            auditLog(
                AuditEvent(
                    eventName,
                    listOf(
                        logField(StructuredLogKeys.ACCOUNT, event.account),
                        logField(StructuredLogKeys.OPERATION, event.operation.name.lowercase()),
                        logField(StructuredLogKeys.OUTCOME, event.outcome.wireValue())
                    )
                )
            )
        }
    }
}

private fun MutationAuditOutcome.wireValue(): String = when (this) {
    is MutationAuditOutcome.Completed -> when (result) {
        MutationAuditResult.STORED -> "stored"
        MutationAuditResult.CONFLICT -> "conflict"
        MutationAuditResult.REMOVED -> "removed"
    }
}
