package f95gm.server.application

import f95gm.server.kernel.ServerClock

import f95gm.domain.api.SavedFilterRequest
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.EpochMillis
import f95gm.server.model.filters.SavedFilter
import f95gm.server.persistence.SavedFilterRepository
import f95gm.server.persistence.valueOrThrow
import f95gm.server.application.port.SavedFilterApplicationPort
import f95gm.server.application.port.ImportResult
import f95gm.server.application.port.MutationAuditKind
import f95gm.server.application.port.MutationAuditEvent
import f95gm.server.application.port.MutationAuditOperation
import f95gm.server.application.port.MutationAuditResult
import f95gm.server.application.port.MutationAuditOutcome
import f95gm.server.application.port.MutationAuditSink
import f95gm.server.application.port.NoopMutationAuditSink

internal class FilterApplicationService(
    val repository: SavedFilterRepository,
    private val auditSink: MutationAuditSink = NoopMutationAuditSink
) : SavedFilterApplicationPort {
    override suspend fun load(account: F95AccountKey): List<SavedFilter> = repository.load(account)

    override fun fromRequest(request: SavedFilterRequest, clock: ServerClock): SavedFilter? = request.toSavedFilter(clock)

    override suspend fun storeAtomically(account: F95AccountKey, filter: SavedFilter, expectedUpdatedAt: EpochMillis?): ConditionalWriteResult<SavedFilter> {
        val result = repository.storeIfCurrentResult(account, filter, expectedUpdatedAt).valueOrThrow()
        auditSink.record(MutationAuditEvent(MutationAuditKind.FILTER, account.value, MutationAuditOperation.STORE, MutationAuditOutcome.Completed(result.toMutationAuditResult())))
        return result
    }

    override suspend fun importAtomically(account: F95AccountKey, filter: SavedFilter): SavedFilter {
        val stored = repository.store(account, filter)
        auditSink.record(MutationAuditEvent(MutationAuditKind.FILTER, account.value, MutationAuditOperation.IMPORT, MutationAuditOutcome.Completed(MutationAuditResult.STORED)))
        return stored
    }

    override suspend fun importAtomically(account: F95AccountKey, filters: List<SavedFilter>): ImportResult {
        val stored = repository.storeAll(account, filters)
        auditSink.record(MutationAuditEvent(MutationAuditKind.FILTER, account.value, MutationAuditOperation.IMPORT, MutationAuditOutcome.Completed(MutationAuditResult.STORED)))
        return ImportResult(stored.size)
    }

    override suspend fun removeAtomically(account: F95AccountKey, filterId: F95FilterId) {
        repository.remove(account, filterId)
        auditSink.record(MutationAuditEvent(MutationAuditKind.FILTER, account.value, MutationAuditOperation.REMOVE, MutationAuditOutcome.Completed(MutationAuditResult.REMOVED)))
    }
}
