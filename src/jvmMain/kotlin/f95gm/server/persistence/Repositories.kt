package f95gm.server.persistence

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import f95gm.server.model.filters.SavedFilter
import f95gm.server.model.marks.GameMark
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.EpochMillis
import f95gm.server.persistence.filters.loadSavedFilters
import f95gm.server.persistence.filters.removeSavedFilter
import f95gm.server.persistence.filters.storeSavedFilter
import f95gm.server.persistence.filters.storeSavedFilters
import f95gm.server.persistence.filters.storeSavedFilterIfCurrent
import f95gm.server.persistence.marks.loadMarks
import f95gm.server.persistence.marks.removeMark
import f95gm.server.persistence.marks.storeMark
import f95gm.server.persistence.marks.storeMarkIfCurrent
import java.sql.SQLException

internal sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data class Failure(val cause: RepositoryFailure) : RepositoryResult<Nothing>
}

internal enum class RepositoryFailure { DATABASE, CONSTRAINT, UNAVAILABLE }

internal class RepositoryOperationException(val failure: RepositoryFailure) : IllegalStateException(failure.name)

internal fun <T> RepositoryResult<T>.valueOrThrow(): T = when (this) {
    is RepositoryResult.Success -> value
    is RepositoryResult.Failure -> throw RepositoryOperationException(cause)
}

internal interface MarkReader {
    suspend fun load(account: F95AccountKey): List<GameMark>
}

internal interface MarkWriter {
    suspend fun store(account: F95AccountKey, mark: GameMark)
    suspend fun remove(account: F95AccountKey, threadId: F95ThreadId)
    suspend fun storeIfCurrent(
        account: F95AccountKey,
        mark: GameMark,
        expectedUpdatedAt: EpochMillis?
    ): ConditionalWriteResult<Unit>

    suspend fun storeIfCurrentResult(
        account: F95AccountKey,
        mark: GameMark,
        expectedUpdatedAt: EpochMillis?
    ): RepositoryResult<ConditionalWriteResult<Unit>> = repositoryResult {
        storeIfCurrent(account, mark, expectedUpdatedAt)
    }
}

internal interface MarkRepository : MarkReader, MarkWriter

internal interface SavedFilterReader {
    suspend fun load(account: F95AccountKey): List<SavedFilter>
}

internal interface SavedFilterWriter {
    suspend fun store(account: F95AccountKey, filter: SavedFilter): SavedFilter
    /** Stores the batch as one transaction; implementations must not fall back to per-item writes. */
    suspend fun storeAll(account: F95AccountKey, filters: List<SavedFilter>): List<SavedFilter>
    suspend fun storeIfCurrent(
        account: F95AccountKey,
        filter: SavedFilter,
        expectedUpdatedAt: EpochMillis?
    ): ConditionalWriteResult<SavedFilter>

    suspend fun storeIfCurrentResult(
        account: F95AccountKey,
        filter: SavedFilter,
        expectedUpdatedAt: EpochMillis?
    ): RepositoryResult<ConditionalWriteResult<SavedFilter>> = repositoryResult {
        storeIfCurrent(account, filter, expectedUpdatedAt)
    }

    suspend fun remove(account: F95AccountKey, filterId: F95FilterId)
}

internal interface SavedFilterRepository : SavedFilterReader, SavedFilterWriter

private suspend inline fun <T> repositoryResult(crossinline block: suspend () -> T): RepositoryResult<T> = try {
    RepositoryResult.Success(block())
} catch (error: SQLException) {
    RepositoryResult.Failure(
        if (error.sqlState?.startsWith("23") == true) RepositoryFailure.CONSTRAINT
        else RepositoryFailure.DATABASE
    )
}

private class ExposedMarkRepository(private val database: TransactionBoundary) : MarkRepository {
    override suspend fun load(account: F95AccountKey) = loadMarks(database, account)
    override suspend fun store(account: F95AccountKey, mark: GameMark) = storeMark(database, account, mark)
    override suspend fun remove(account: F95AccountKey, threadId: F95ThreadId) = removeMark(database, account, threadId)
    override suspend fun storeIfCurrent(
        account: F95AccountKey,
        mark: GameMark,
        expectedUpdatedAt: EpochMillis?
    ): ConditionalWriteResult<Unit> {
        return storeMarkIfCurrent(database, account, mark, expectedUpdatedAt)
    }
}

private class ExposedSavedFilterRepository(private val database: TransactionBoundary) : SavedFilterRepository {
    override suspend fun load(account: F95AccountKey) = loadSavedFilters(database, account)
    override suspend fun store(account: F95AccountKey, filter: SavedFilter) =
        storeSavedFilter(database, account, filter)

    override suspend fun storeAll(account: F95AccountKey, filters: List<SavedFilter>) =
        storeSavedFilters(database, account, filters)

    override suspend fun storeIfCurrent(account: F95AccountKey, filter: SavedFilter, expectedUpdatedAt: EpochMillis?) =
        storeSavedFilterIfCurrent(database, account, filter, expectedUpdatedAt)

    override suspend fun remove(account: F95AccountKey, filterId: F95FilterId) =
        removeSavedFilter(database, account, filterId)
}

internal class ServerRepositories(
    database: TransactionBoundary,
    val marks: MarkRepository = ExposedMarkRepository(database),
    val savedFilters: SavedFilterRepository = ExposedSavedFilterRepository(database)
)
