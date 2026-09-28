package f95gm.server.application

import f95gm.domain.api.F95MarkRequest
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.EpochMillis
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.persistence.MarkRepository
import f95gm.server.persistence.valueOrThrow
import f95gm.server.application.port.MarkApplicationPort
import f95gm.server.application.port.MutationAuditKind
import f95gm.server.application.port.MutationAuditEvent
import f95gm.server.application.port.MutationAuditOperation
import f95gm.server.application.port.MutationAuditResult
import f95gm.server.application.port.MutationAuditOutcome
import f95gm.server.application.port.MutationAuditSink
import f95gm.server.application.port.NoopMutationAuditSink

internal class MarkApplicationService(
    private val repository: MarkRepository,
    private val auditSink: MutationAuditSink = NoopMutationAuditSink
) : MarkApplicationPort {
    override suspend fun load(account: F95AccountKey): List<GameMark> = repository.load(account)

    override suspend fun resetTrackingStateForNewVersion(
        account: F95AccountKey,
        marks: List<GameMark>,
        live: Map<F95ThreadId, LiveMarkInfo>
    ): List<GameMark> {
        val updated = marks.map { mark ->
            val latestVersion = live[mark.threadId]?.version?.value ?: F95Defaults.UNKNOWN_VERSION
            val newVersionDetected = f95gm.server.model.version.isNewerVersion(latestVersion, mark.storedVersion.value) &&
                !latestVersion.equals(mark.lastSeenUpdateVersion.value, ignoreCase = true)
            if (!newVersionDetected) mark else mark.copy(
                trackingState = GameTrackingState.WATCHING,
                lastSeenUpdateVersion = StoredVersion(latestVersion)
            )
        }
        updated.zip(marks).forEach { (newMark, oldMark) -> if (newMark != oldMark) repository.store(account, newMark) }
        return updated
    }

    override fun createMark(threadId: F95ThreadId, request: F95MarkRequest, existing: GameMark?, now: EpochMillis): GameMark {
        val requestedStoredVersion = request.storedVersion.value.trim()
            .take(f95gm.server.config.CatalogLimits.MAX_TEXT_LENGTH)
            .ifBlank { existing?.storedVersion?.value ?: F95Defaults.UNKNOWN_VERSION }
        return GameMark(
            threadId, GameTrackingState.fromWire(request.trackingState?.wireValue ?: "") ?: GameTrackingState.WATCHING,
            if (request.acknowledge) StoredVersion(requestedStoredVersion) else existing?.storedVersion ?: StoredVersion(requestedStoredVersion),
            existing?.lastSeenUpdateVersion ?: StoredVersion(F95Defaults.EMPTY), now
        )
    }

    override suspend fun storeAtomically(account: F95AccountKey, mark: GameMark, expectedUpdatedAt: EpochMillis?): ConditionalWriteResult<Unit> {
        val result = repository.storeIfCurrentResult(account, mark, expectedUpdatedAt).valueOrThrow()
        auditSink.record(MutationAuditEvent(MutationAuditKind.MARK, account.value, MutationAuditOperation.STORE, MutationAuditOutcome.Completed(result.toMutationAuditResult())))
        return result
    }

    override suspend fun removeAtomically(account: F95AccountKey, threadId: F95ThreadId) {
        repository.remove(account, threadId)
        auditSink.record(MutationAuditEvent(MutationAuditKind.MARK, account.value, MutationAuditOperation.REMOVE, MutationAuditOutcome.Completed(MutationAuditResult.REMOVED)))
    }
}
