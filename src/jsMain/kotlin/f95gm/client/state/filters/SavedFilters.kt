package f95gm.client.state.filters

import dev.fritz2.core.RootStore
import f95gm.client.model.filters.SavedFiltersState
import kotlinx.coroutines.Job

internal val savedFilters = RootStore(SavedFiltersState(), Job())
