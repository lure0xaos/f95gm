package f95gm.client.state.runtime

import f95gm.domain.values.F95FilterId

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
    override fun applyFilterRouteIfNeeded(filterId: String?) = f95gm.client.actions.filters.applyFilterRouteIfNeeded(actions, filterId)
    override fun deleteSavedFilter(filterId: F95FilterId) = f95gm.client.actions.filters.deleteSavedFilter(actions, filterId)
    override fun exportSavedFilters() = f95gm.client.actions.filters.exportSavedFilters(actions)
    override fun importSavedFilters() = f95gm.client.actions.filters.importSavedFilters(actions)
    override fun loadSavedFiltersIfNeeded() = f95gm.client.actions.filters.loadSavedFiltersIfNeeded(actions)
    override fun retrySavedFilters() = f95gm.client.actions.filters.retrySavedFilters(actions)
    override fun saveCurrentFilter() = f95gm.client.actions.filters.saveCurrentFilter(actions)
    override fun updateSavedFilterName(name: String) = f95gm.client.actions.filters.updateSavedFilterName(actions, name)
}
