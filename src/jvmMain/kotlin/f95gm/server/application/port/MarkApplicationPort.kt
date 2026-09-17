package f95gm.server.application.port

import f95gm.domain.api.F95MarkRequest
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95ThreadId
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.EpochMillis
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo

internal interface MarkApplicationPort {
    suspend fun load(account: F95AccountKey): List<GameMark>
    suspend fun resetTrackingStateForNewVersion(account: F95AccountKey, marks: List<GameMark>, live: Map<F95ThreadId, LiveMarkInfo>): List<GameMark>
    fun createMark(threadId: F95ThreadId, request: F95MarkRequest, existing: GameMark?, now: EpochMillis): GameMark
    suspend fun storeAtomically(account: F95AccountKey, mark: GameMark, expectedUpdatedAt: EpochMillis?): ConditionalWriteResult<Unit>
    suspend fun removeAtomically(account: F95AccountKey, threadId: F95ThreadId)
}
