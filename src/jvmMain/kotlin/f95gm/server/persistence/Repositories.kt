package f95gm.server.persistence

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import f95gm.server.model.filters.SavedFilter
import f95gm.server.model.marks.GameMark
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.filters.loadSavedFilters
import f95gm.server.persistence.filters.removeSavedFilter
import f95gm.server.persistence.filters.storeSavedFilter
import f95gm.server.persistence.filters.storeSavedFilterIfCurrent
import f95gm.server.persistence.marks.loadMarks
import f95gm.server.persistence.marks.removeMark
import f95gm.server.persistence.marks.storeMark
import f95gm.server.persistence.marks.storeMarkIfCurrent

internal interface MarkRepository {
    suspend fun load(account: F95AccountKey): List<GameMark>
    suspend fun store(account: F95AccountKey, mark: GameMark)
    suspend fun remove(account: F95AccountKey, threadId: F95ThreadId)
    suspend fun storeIfCurrent(
        account: F95AccountKey,
        mark: GameMark,
        expectedUpdatedAt: Long?
    ): ConditionalWriteResult<Unit>
}

internal interface SavedFilterRepository {
    suspend fun load(account: F95AccountKey): List<SavedFilter>
    suspend fun store(account: F95AccountKey, filter: SavedFilter): SavedFilter
    suspend fun storeIfCurrent(
        account: F95AccountKey,
        filter: SavedFilter,
        expectedUpdatedAt: Long?
    ): ConditionalWriteResult<SavedFilter>

    suspend fun remove(account: F95AccountKey, filterId: F95FilterId)
}

private class ExposedMarkRepository(private val database: DatabaseExecutor) : MarkRepository {
    override suspend fun load(account: F95AccountKey) = loadMarks(database, account)
    override suspend fun store(account: F95AccountKey, mark: GameMark) = storeMark(database, account, mark)
    override suspend fun remove(account: F95AccountKey, threadId: F95ThreadId) = removeMark(database, account, threadId)
    override suspend fun storeIfCurrent(
        account: F95AccountKey,
        mark: GameMark,
        expectedUpdatedAt: Long?
    ): ConditionalWriteResult<Unit> {
        return storeMarkIfCurrent(database, account, mark, expectedUpdatedAt)
    }
}

private class ExposedSavedFilterRepository(private val database: DatabaseExecutor) : SavedFilterRepository {
    override suspend fun load(account: F95AccountKey) = loadSavedFilters(database, account)
    override suspend fun store(account: F95AccountKey, filter: SavedFilter) =
        storeSavedFilter(database, account, filter)

    override suspend fun storeIfCurrent(account: F95AccountKey, filter: SavedFilter, expectedUpdatedAt: Long?) =
        storeSavedFilterIfCurrent(database, account, filter, expectedUpdatedAt)

    override suspend fun remove(account: F95AccountKey, filterId: F95FilterId) =
        removeSavedFilter(database, account, filterId)
}

internal class ServerRepositories(
    database: DatabaseExecutor,
    val marks: MarkRepository = ExposedMarkRepository(database),
    val savedFilters: SavedFilterRepository = ExposedSavedFilterRepository(database)
)
