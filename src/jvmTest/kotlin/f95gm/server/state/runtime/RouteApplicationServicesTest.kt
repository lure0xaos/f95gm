package f95gm.server.state.runtime

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import f95gm.server.http.request.MutationService
import f95gm.server.model.filters.SavedFilter
import f95gm.server.model.marks.GameMark
import f95gm.server.persistence.ConditionalWriteResult
import f95gm.server.persistence.MarkRepository
import f95gm.server.persistence.SavedFilterRepository
import f95gm.server.persistence.ServerRepositories
import f95gm.server.persistence.database.DatabaseExecutor
import kotlin.test.Test
import kotlin.test.assertSame

private class FakeMarks : MarkRepository {
    override suspend fun load(account: F95AccountKey) = emptyList<GameMark>()
    override suspend fun store(account: F95AccountKey, mark: GameMark) = Unit
    override suspend fun remove(account: F95AccountKey, threadId: F95ThreadId) = Unit
    override suspend fun storeIfCurrent(
        account: F95AccountKey,
        mark: GameMark,
        expectedUpdatedAt: Long?
    ): ConditionalWriteResult<Unit> =
        ConditionalWriteResult.Stored(Unit)
}

private class FakeFilters : SavedFilterRepository {
    override suspend fun load(account: F95AccountKey) = emptyList<SavedFilter>()
    override suspend fun store(account: F95AccountKey, filter: SavedFilter) = filter
    override suspend fun storeIfCurrent(
        account: F95AccountKey,
        filter: SavedFilter,
        expectedUpdatedAt: Long?
    ): ConditionalWriteResult<SavedFilter> =
        ConditionalWriteResult.Stored(filter)

    override suspend fun remove(account: F95AccountKey, filterId: F95FilterId) = Unit
}

class RouteApplicationServicesTest {
    @Test
    fun exposesRepositoryContractsAndMutationServiceWithoutDatabaseTypes() {
        val marks = FakeMarks()
        val filters = FakeFilters()
        val mutations = MutationService(IdempotencyRegistry())
        val services = DefaultRouteApplicationServices(
            ServerRepositories(
                database = DatabaseExecutor(database = { error("not used") }), marks = marks, savedFilters = filters
            ), mutations
        )

        assertSame(marks, services.marks)
        assertSame(filters, services.savedFilters)
        assertSame(mutations, services.mutations)
    }
}
