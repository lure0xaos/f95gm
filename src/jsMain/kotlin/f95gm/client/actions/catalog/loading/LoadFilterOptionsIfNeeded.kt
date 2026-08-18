package f95gm.client.actions.catalog.loading

import f95gm.client.state.catalog.catalog
import f95gm.client.state.catalog.catalogOptions
import f95gm.client.state.catalog.loadedOptionsCategory
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun loadFilterOptionsIfNeeded() {
    scope.launch {
        val category = catalog.data.first().category
        if (loadedOptionsCategory == category.wireValue && catalogOptions.data.first().error.isBlank()) return@launch
        loadedOptionsCategory = category.wireValue
        loadFilterOptions(category)
    }
}
