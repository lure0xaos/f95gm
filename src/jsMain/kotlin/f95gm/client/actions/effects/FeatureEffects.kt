package f95gm.client.actions.effects

import f95gm.client.model.catalog.CatalogOptions
import f95gm.client.model.catalog.CatalogState
import f95gm.client.model.detail.DetailState
import f95gm.client.model.filters.SavedFiltersState
import f95gm.client.model.marks.MarksState
import dev.fritz2.core.RootStore

internal object CatalogEffects {
    suspend fun reset(catalog: RootStore<CatalogState>, catalogOptions: RootStore<CatalogOptions>) {
        catalog.enqueue { CatalogState() }
        catalogOptions.enqueue { CatalogOptions() }
    }
}

internal object FilterEffects {
    suspend fun reset(savedFilters: RootStore<SavedFiltersState>, savedFilterName: RootStore<String>) {
        savedFilters.enqueue { SavedFiltersState() }
        savedFilterName.enqueue { "" }
    }
}

internal object MarkEffects {
    suspend fun reset(marks: RootStore<MarksState>) = marks.enqueue { MarksState() }
}

internal object DetailEffects {
    suspend fun reset(detail: RootStore<DetailState>) = detail.enqueue { DetailState() }
}
