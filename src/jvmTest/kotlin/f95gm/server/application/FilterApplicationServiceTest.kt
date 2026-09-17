package f95gm.server.application

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.server.application.port.MutationAuditEvent
import f95gm.server.application.port.MutationAuditSink
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.EpochMillis
import f95gm.server.model.filters.SavedFilter
import f95gm.server.persistence.SavedFilterRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FilterApplicationServiceTest {
    @Test
    fun delegatesWritesAndEmitsAuditEventsForEachMutation() = runTest {
        val repository = RecordingFilterRepository()
        val events = mutableListOf<MutationAuditEvent>()
        val service = FilterApplicationService(repository, MutationAuditSink(events::add))
        val account = F95AccountKey("account")
        val filter = SavedFilter(id = F95FilterId("favorites"), name = f95gm.domain.values.F95OptionName("Favorites"))

        assertEquals(emptyList(), service.load(account))
        assertEquals(filter, service.importAtomically(account, filter))
        assertEquals(2, service.importAtomically(account, listOf(filter, filter)).imported)
        assertIs<ConditionalWriteResult.Stored<SavedFilter>>(service.storeAtomically(account, filter, null))
        service.removeAtomically(account, filter.id)

        assertEquals(listOf("IMPORT", "IMPORT", "STORE", "REMOVE"), events.map { it.operation.name })
        assertEquals(listOf("favorites"), repository.removed)
        assertEquals(0, service.load(account).size)
    }
}

private class RecordingFilterRepository : SavedFilterRepository {
    private val values = linkedMapOf<String, SavedFilter>()
    val removed = mutableListOf<String>()

    override suspend fun load(account: F95AccountKey) = values.values.toList()
    override suspend fun store(account: F95AccountKey, filter: SavedFilter): SavedFilter = filter.also { values[it.id.value] = it }
    override suspend fun storeAll(account: F95AccountKey, filters: List<SavedFilter>): List<SavedFilter> = filters.onEach { values[it.id.value] = it }
    override suspend fun storeIfCurrent(account: F95AccountKey, filter: SavedFilter, expectedUpdatedAt: EpochMillis?): ConditionalWriteResult<SavedFilter> =
        ConditionalWriteResult.Stored(store(account, filter))
    override suspend fun remove(account: F95AccountKey, filterId: F95FilterId) {
        removed += filterId.value
        values.remove(filterId.value)
    }
}
