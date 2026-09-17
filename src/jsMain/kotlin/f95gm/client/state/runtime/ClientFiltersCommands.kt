package f95gm.client.state.runtime

import f95gm.domain.values.F95FilterId
import f95gm.client.actions.filters.applyFilterRouteIfNeeded as applyFilterRouteIfNeededFromFilters
import f95gm.client.actions.filters.deleteSavedFilter as deleteSavedFilterFromFilters
import f95gm.client.actions.filters.exportSavedFilters as exportSavedFiltersFromFilters
import f95gm.client.actions.filters.importSavedFilters as importSavedFiltersFromFilters
import f95gm.client.actions.filters.loadSavedFiltersIfNeeded as loadSavedFiltersIfNeededFromFilters
import f95gm.client.actions.filters.retrySavedFilters as retrySavedFiltersFromFilters
import f95gm.client.actions.filters.saveCurrentFilter as saveCurrentFilterFromFilters
import f95gm.client.actions.filters.updateSavedFilterName as updateSavedFilterNameFromFilters

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientFiltersCommands {
    fun applyFilterRouteIfNeeded(filterId: String?)
    fun deleteSavedFilter(filterId: F95FilterId)
    fun exportSavedFilters()
    fun importSavedFilters()
    fun loadSavedFiltersIfNeeded()
    fun retrySavedFilters()
    fun saveCurrentFilter()
    fun updateSavedFilterName(name: String)
}

internal class ClientFiltersCommandsAdapter(private val actions: ClientFiltersActionsPort) : ClientFiltersCommands {
    override fun applyFilterRouteIfNeeded(filterId: String?) = applyFilterRouteIfNeededFromFilters(actions, filterId)
    override fun deleteSavedFilter(filterId: F95FilterId) = deleteSavedFilterFromFilters(actions, filterId)
    override fun exportSavedFilters() = exportSavedFiltersFromFilters(actions)
    override fun importSavedFilters() = importSavedFiltersFromFilters(actions)
    override fun loadSavedFiltersIfNeeded() = loadSavedFiltersIfNeededFromFilters(actions)
    override fun retrySavedFilters() = retrySavedFiltersFromFilters(actions)
    override fun saveCurrentFilter() = saveCurrentFilterFromFilters(actions)
    override fun updateSavedFilterName(name: String) = updateSavedFilterNameFromFilters(actions, name)
}
