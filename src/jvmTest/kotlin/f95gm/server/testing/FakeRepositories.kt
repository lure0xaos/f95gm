package f95gm.server.testing

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import f95gm.server.model.filters.SavedFilter
import f95gm.server.model.marks.GameMark
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.EpochMillis
import f95gm.server.persistence.MarkRepository
import f95gm.server.persistence.SavedFilterRepository

/** In-memory ports for route/application tests; no database or Exposed types leak into tests. */
internal class FakeMarkRepository : MarkRepository {
    private val values = mutableMapOf<String, MutableMap<String, GameMark>>()
    private val lock = Any()

    override suspend fun load(account: F95AccountKey): List<GameMark> = synchronized(lock) {
        values[account.value].orEmpty().values.toList()
    }

    override suspend fun store(account: F95AccountKey, mark: GameMark) {
        synchronized(lock) {
            values.getOrPut(account.value, ::mutableMapOf)[mark.threadId.value] = mark
        }
    }

    override suspend fun remove(account: F95AccountKey, threadId: F95ThreadId) {
        synchronized(lock) {
            values[account.value]?.remove(threadId.value)
        }
    }

    override suspend fun storeIfCurrent(
        account: F95AccountKey,
        mark: GameMark,
        expectedUpdatedAt: EpochMillis?
    ): ConditionalWriteResult<Unit> {
        return synchronized(lock) {
            val current = values[account.value]?.get(mark.threadId.value)
            if (expectedUpdatedAt != null && current?.updatedAt != expectedUpdatedAt) {
                ConditionalWriteResult.Conflict(expectedUpdatedAt, null)
            } else {
                values.getOrPut(account.value, ::mutableMapOf)[mark.threadId.value] = mark
                ConditionalWriteResult.Stored(Unit)
            }
        }
    }
}

internal class FakeSavedFilterRepository : SavedFilterRepository {
    private val values = mutableMapOf<String, MutableMap<String, SavedFilter>>()
    private val lock = Any()

    override suspend fun load(account: F95AccountKey): List<SavedFilter> = synchronized(lock) {
        values[account.value].orEmpty().values.toList()
    }

    override suspend fun store(account: F95AccountKey, filter: SavedFilter): SavedFilter {
        return synchronized(lock) {
            values.getOrPut(account.value, ::mutableMapOf)[filter.id.value] = filter
            filter
        }
    }

    override suspend fun storeAll(account: F95AccountKey, filters: List<SavedFilter>): List<SavedFilter> =
        synchronized(lock) {
            val updated = values[account.value].orEmpty().toMutableMap()
            filters.forEach { updated[it.id.value] = it }
            values[account.value] = updated
            filters
        }

    override suspend fun storeIfCurrent(
        account: F95AccountKey,
        filter: SavedFilter,
        expectedUpdatedAt: EpochMillis?
    ): ConditionalWriteResult<SavedFilter> = synchronized(lock) {
        val current = values[account.value]?.get(filter.id.value)
        if (expectedUpdatedAt != null && current?.updatedAt != expectedUpdatedAt) {
            ConditionalWriteResult.Conflict(expectedUpdatedAt, null)
        } else {
            values.getOrPut(account.value, ::mutableMapOf)[filter.id.value] = filter
            ConditionalWriteResult.Stored(filter)
        }
    }

    override suspend fun remove(account: F95AccountKey, filterId: F95FilterId) {
        synchronized(lock) {
            values[account.value]?.remove(filterId.value)
        }
    }
}
