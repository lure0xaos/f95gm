package f95gm.client.model.catalog

import f95gm.client.data.catalog.CatalogResult

internal sealed interface CatalogEvent {
    data object BeginLoad : CatalogEvent
    data class Loaded(val result: CatalogResult, val items: List<Game>) : CatalogEvent
    data class Failed(val message: String) : CatalogEvent
    data object ResetSearch : CatalogEvent
    data class PageChanged(val page: Int) : CatalogEvent
}
