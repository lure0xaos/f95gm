package f95gm.server.application

import f95gm.server.application.port.MutationAuditResult
import f95gm.server.model.ConditionalWriteResult

internal fun ConditionalWriteResult<*>.toMutationAuditResult(): MutationAuditResult = when (this) {
    is ConditionalWriteResult.Stored -> MutationAuditResult.STORED
    is ConditionalWriteResult.Conflict -> MutationAuditResult.CONFLICT
}
