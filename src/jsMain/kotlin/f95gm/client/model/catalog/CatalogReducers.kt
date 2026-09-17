package f95gm.client.model.catalog

import f95gm.client.data.catalog.CatalogResult
import f95gm.client.model.runtime.CatalogSearchQuery
import f95gm.client.model.runtime.DateLimit
import f95gm.domain.defaults.F95Defaults
import f95gm.client.state.runtime.AsyncState

internal fun CatalogState.reduce(event: CatalogEvent): CatalogState = when (event) {
    CatalogEvent.BeginLoad -> copy(async = AsyncState.Loading)
    is CatalogEvent.Loaded -> copy(
        items = event.items,
        totalPages = event.result.totalPages,
        async = AsyncState.Success(Unit)
    )
    is CatalogEvent.Failed -> copy(async = AsyncState.Failure(event.message))
    CatalogEvent.ResetSearch -> copy(
        search = CatalogSearchQuery.EMPTY,
        creatorSearch = CatalogSearchQuery.EMPTY,
        dateLimit = DateLimit.ANY,
        page = F95Defaults.FIRST_PAGE
    )
    is CatalogEvent.PageChanged -> copy(page = event.page, pageInput = F95Defaults.EMPTY)
}

internal fun CatalogState.beginCatalogLoad() = reduce(CatalogEvent.BeginLoad)

internal fun CatalogState.completeCatalogLoad(result: CatalogResult, items: List<Game>) = reduce(CatalogEvent.Loaded(result, items))

internal fun CatalogState.failCatalogLoad(message: String) = reduce(CatalogEvent.Failed(message))

internal fun CatalogState.resetCatalogSearch(): CatalogState = reduce(CatalogEvent.ResetSearch)

internal fun CatalogState.withCatalogPage(page: Int): CatalogState = reduce(CatalogEvent.PageChanged(page))
