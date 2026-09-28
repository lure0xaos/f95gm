package f95gm.client.state.runtime

import f95gm.client.model.catalog.FilterSelection
import f95gm.client.ui.shared.pagination.PageNumber
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95OptionId

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientCatalogCommands {
    fun loadCatalogIfNeeded()
    fun loadFilterOptionsIfNeeded()
    fun reloadCatalog()
    fun retryCatalog()
    fun retryFilterOptions()
    fun closeCatalogPageEdit()
    fun movePage(delta: Int)
    fun movePageTo(page: Int)
    fun movePageTo(page: PageNumber)
    fun startCatalogPageEdit()
    fun submitCatalogPageInput()
    fun updateCatalogPageInput(input: String)
    fun applyFilters()
    fun clearFilters()
    fun removePrefix(id: String, selection: FilterSelection)
    fun removeTag(id: String, selection: FilterSelection)
    fun toggleFilterPanel()
    fun updateCatalogSearch(search: String)
    fun updateCreatorSearch(search: String)
    fun selectCategory(category: F95Category)
    fun selectDateLimit(value: Int)
    fun selectExcludedPrefixPicker(value: String, groupId: F95OptionId)
    fun selectExcludedTagPicker(value: String)
    fun selectPrefixPicker(value: String, groupId: F95OptionId)
    fun selectSearchMode(mode: F95SearchMode)
    fun selectSort(sort: F95Sort)
    fun selectTagPicker(value: String)
    fun selectTagType(value: F95TagType)
}

internal class ClientCatalogCommandsAdapter(private val actions: ClientCatalogBootstrapActionsPort) : ClientCatalogCommands {
    override fun loadCatalogIfNeeded() = f95gm.client.actions.catalog.loading.loadCatalogIfNeeded(actions)
    override fun loadFilterOptionsIfNeeded() = f95gm.client.actions.catalog.loading.loadFilterOptionsIfNeeded(actions)
    override fun reloadCatalog() = f95gm.client.actions.catalog.loading.reloadCatalog(actions)
    override fun retryCatalog() = f95gm.client.actions.catalog.loading.retryCatalog(actions)
    override fun retryFilterOptions() = f95gm.client.actions.catalog.loading.retryFilterOptions(actions)
    override fun closeCatalogPageEdit() = f95gm.client.actions.catalog.pagination.closeCatalogPageEdit(actions)
    override fun movePage(delta: Int) = f95gm.client.actions.catalog.pagination.movePage(actions, delta)
    override fun movePageTo(page: Int) = f95gm.client.actions.catalog.pagination.movePageTo(actions, page)
    override fun movePageTo(page: PageNumber) = f95gm.client.actions.catalog.pagination.movePageTo(actions, page)
    override fun startCatalogPageEdit() = f95gm.client.actions.catalog.pagination.startCatalogPageEdit(actions)
    override fun submitCatalogPageInput() = f95gm.client.actions.catalog.pagination.submitCatalogPageInput(actions)
    override fun updateCatalogPageInput(input: String) = f95gm.client.actions.catalog.pagination.updateCatalogPageInput(actions, input)
    override fun applyFilters() = f95gm.client.actions.catalog.filters.editing.applyFilters(actions)
    override fun clearFilters() = f95gm.client.actions.catalog.filters.editing.clearFilters(actions)
    override fun removePrefix(id: String, selection: FilterSelection) = f95gm.client.actions.catalog.filters.editing.removePrefix(actions, id, selection)
    override fun removeTag(id: String, selection: FilterSelection) = f95gm.client.actions.catalog.filters.editing.removeTag(actions, id, selection)
    override fun toggleFilterPanel() = f95gm.client.actions.catalog.filters.editing.toggleFilterPanel(actions)
    override fun updateCatalogSearch(search: String) = f95gm.client.actions.catalog.filters.search.updateCatalogSearch(actions, search)
    override fun updateCreatorSearch(search: String) = f95gm.client.actions.catalog.filters.search.updateCreatorSearch(actions, search)
    override fun selectCategory(category: F95Category) = f95gm.client.actions.catalog.filters.selection.selectCategory(actions, category)
    override fun selectDateLimit(value: Int) = f95gm.client.actions.catalog.filters.selection.selectDateLimit(actions, value)
    override fun selectExcludedPrefixPicker(value: String, groupId: F95OptionId) = f95gm.client.actions.catalog.filters.selection.selectExcludedPrefixPicker(actions, value, groupId)
    override fun selectExcludedTagPicker(value: String) = f95gm.client.actions.catalog.filters.selection.selectExcludedTagPicker(actions, value)
    override fun selectPrefixPicker(value: String, groupId: F95OptionId) = f95gm.client.actions.catalog.filters.selection.selectPrefixPicker(actions, value, groupId)
    override fun selectSearchMode(mode: F95SearchMode) = f95gm.client.actions.catalog.filters.selection.selectSearchMode(actions, mode)
    override fun selectSort(sort: F95Sort) = f95gm.client.actions.catalog.filters.selection.selectSort(actions, sort)
    override fun selectTagPicker(value: String) = f95gm.client.actions.catalog.filters.selection.selectTagPicker(actions, value)
    override fun selectTagType(value: F95TagType) = f95gm.client.actions.catalog.filters.selection.selectTagType(actions, value)
}
