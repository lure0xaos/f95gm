package f95gm.client.state.runtime

import dev.fritz2.core.RootStore
import f95gm.client.model.catalog.CatalogOptions
import f95gm.client.model.catalog.CatalogState
import f95gm.client.model.detail.DetailState
import f95gm.client.model.filters.SavedFiltersState
import f95gm.client.model.images.ImageRetryState
import f95gm.client.model.marks.MarksState
import f95gm.client.state.dropdown.DropdownSearchState
import kotlinx.coroutines.Job

/** Groups feature-owned stores while keeping the runtime facade stable for callers. */
internal class ClientFeatureStores(storeJob: Job) {
    val catalog = RootStore(CatalogState(), storeJob)
    val catalogOptions = RootStore(CatalogOptions(), storeJob)
    val savedFilters = RootStore(SavedFiltersState(), storeJob)
    val savedFilterName = RootStore("", storeJob)
    val marks = RootStore(MarksState(), storeJob)
    val detail = RootStore(DetailState(), storeJob)
    val imageRetry = RootStore(ImageRetryState(), storeJob)
    val dropdownSearches = RootStore(DropdownSearchState(), storeJob)
}
