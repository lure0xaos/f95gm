package f95gm.client.model.catalog

import f95gm.client.data.catalog.CatalogResult
import f95gm.client.model.runtime.CatalogSearchQuery
import f95gm.client.model.runtime.DateLimit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CatalogReducersTest {
    @Test
    fun resetSearchEventClearsAllSearchInputsAndReturnsToTheFirstPage() {
        val state = CatalogState(
            page = 7,
            search = CatalogSearchQuery("title"),
            creatorSearch = CatalogSearchQuery("creator"),
            dateLimit = DateLimit(30)
        )

        val reset = state.reduce(CatalogEvent.ResetSearch)

        assertEquals(CatalogSearchQuery.EMPTY, reset.search)
        assertEquals(CatalogSearchQuery.EMPTY, reset.creatorSearch)
        assertEquals(DateLimit.ANY, reset.dateLimit)
        assertEquals(1, reset.page)
    }

    @Test
    fun loadEventsHaveExplicitAsyncStateTransitions() {
        val loading = CatalogState().reduce(CatalogEvent.BeginLoad)
        assertIs<f95gm.client.state.runtime.AsyncState.Loading>(loading.async)

        val loaded = loading.reduce(CatalogEvent.Loaded(CatalogResult(emptyList(), 4), emptyList()))
        assertEquals(4, loaded.totalPages)
        assertIs<f95gm.client.state.runtime.AsyncState.Success<Unit>>(loaded.async)

        val failed = loaded.reduce(CatalogEvent.Failed("offline"))
        assertEquals("offline", failed.error)
    }

    @Test
    fun pageEventsAreStableForLargePageInputs() {
        val state = CatalogState()
        repeat(10_000) { index ->
            val next = state.reduce(CatalogEvent.PageChanged(index + 1))
            assertEquals(index + 1, next.page)
        }
    }
}
