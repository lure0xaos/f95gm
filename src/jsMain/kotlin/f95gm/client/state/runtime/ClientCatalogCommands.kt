package f95gm.client.state.runtime

import f95gm.client.model.catalog.FilterSelection
import f95gm.client.ui.shared.pagination.PageNumber
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95OptionId
import f95gm.client.actions.catalog.filters.editing.applyFilters as applyFiltersFromEditing
import f95gm.client.actions.catalog.filters.editing.clearFilters as clearFiltersFromEditing
import f95gm.client.actions.catalog.filters.editing.removePrefix as removePrefixFromEditing
import f95gm.client.actions.catalog.filters.editing.removeTag as removeTagFromEditing
import f95gm.client.actions.catalog.filters.editing.toggleFilterPanel as toggleFilterPanelFromEditing
import f95gm.client.actions.catalog.filters.search.updateCatalogSearch as updateCatalogSearchFromSearch
import f95gm.client.actions.catalog.filters.search.updateCreatorSearch as updateCreatorSearchFromSearch
import f95gm.client.actions.catalog.filters.selection.selectCategory as selectCategoryFromSelection
import f95gm.client.actions.catalog.filters.selection.selectDateLimit as selectDateLimitFromSelection
import f95gm.client.actions.catalog.filters.selection.selectExcludedPrefixPicker as selectExcludedPrefixPickerFromSelection
import f95gm.client.actions.catalog.filters.selection.selectExcludedTagPicker as selectExcludedTagPickerFromSelection
import f95gm.client.actions.catalog.filters.selection.selectPrefixPicker as selectPrefixPickerFromSelection
import f95gm.client.actions.catalog.filters.selection.selectSearchMode as selectSearchModeFromSelection
import f95gm.client.actions.catalog.filters.selection.selectSort as selectSortFromSelection
import f95gm.client.actions.catalog.filters.selection.selectTagPicker as selectTagPickerFromSelection
import f95gm.client.actions.catalog.filters.selection.selectTagType as selectTagTypeFromSelection
import f95gm.client.actions.catalog.loading.loadCatalogIfNeeded as loadCatalogIfNeededFromLoading
import f95gm.client.actions.catalog.loading.loadFilterOptionsIfNeeded as loadFilterOptionsIfNeededFromLoading
import f95gm.client.actions.catalog.loading.reloadCatalog as reloadCatalogFromLoading
import f95gm.client.actions.catalog.loading.retryCatalog as retryCatalogFromLoading
import f95gm.client.actions.catalog.loading.retryFilterOptions as retryFilterOptionsFromLoading
import f95gm.client.actions.catalog.pagination.closeCatalogPageEdit as closeCatalogPageEditFromPagination
import f95gm.client.actions.catalog.pagination.movePage as movePageFromPagination
import f95gm.client.actions.catalog.pagination.movePageTo as movePageToFromPagination
import f95gm.client.actions.catalog.pagination.startCatalogPageEdit as startCatalogPageEditFromPagination
import f95gm.client.actions.catalog.pagination.submitCatalogPageInput as submitCatalogPageInputFromPagination
import f95gm.client.actions.catalog.pagination.updateCatalogPageInput as updateCatalogPageInputFromPagination

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
    override fun loadCatalogIfNeeded() = loadCatalogIfNeededFromLoading(actions)
    override fun loadFilterOptionsIfNeeded() = loadFilterOptionsIfNeededFromLoading(actions)
    override fun reloadCatalog() = reloadCatalogFromLoading(actions)
    override fun retryCatalog() = retryCatalogFromLoading(actions)
    override fun retryFilterOptions() = retryFilterOptionsFromLoading(actions)
    override fun closeCatalogPageEdit() = closeCatalogPageEditFromPagination(actions)
    override fun movePage(delta: Int) = movePageFromPagination(actions, delta)
    override fun movePageTo(page: Int) = movePageToFromPagination(actions, page)
    override fun movePageTo(page: PageNumber) = movePageToFromPagination(actions, page)
    override fun startCatalogPageEdit() = startCatalogPageEditFromPagination(actions)
    override fun submitCatalogPageInput() = submitCatalogPageInputFromPagination(actions)
    override fun updateCatalogPageInput(input: String) = updateCatalogPageInputFromPagination(actions, input)
    override fun applyFilters() = applyFiltersFromEditing(actions)
    override fun clearFilters() = clearFiltersFromEditing(actions)
    override fun removePrefix(id: String, selection: FilterSelection) = removePrefixFromEditing(actions, id, selection)
    override fun removeTag(id: String, selection: FilterSelection) = removeTagFromEditing(actions, id, selection)
    override fun toggleFilterPanel() = toggleFilterPanelFromEditing(actions)
    override fun updateCatalogSearch(search: String) = updateCatalogSearchFromSearch(actions, search)
    override fun updateCreatorSearch(search: String) = updateCreatorSearchFromSearch(actions, search)
    override fun selectCategory(category: F95Category) = selectCategoryFromSelection(actions, category)
    override fun selectDateLimit(value: Int) = selectDateLimitFromSelection(actions, value)
    override fun selectExcludedPrefixPicker(value: String, groupId: F95OptionId) = selectExcludedPrefixPickerFromSelection(actions, value, groupId)
    override fun selectExcludedTagPicker(value: String) = selectExcludedTagPickerFromSelection(actions, value)
    override fun selectPrefixPicker(value: String, groupId: F95OptionId) = selectPrefixPickerFromSelection(actions, value, groupId)
    override fun selectSearchMode(mode: F95SearchMode) = selectSearchModeFromSelection(actions, mode)
    override fun selectSort(sort: F95Sort) = selectSortFromSelection(actions, sort)
    override fun selectTagPicker(value: String) = selectTagPickerFromSelection(actions, value)
    override fun selectTagType(value: F95TagType) = selectTagTypeFromSelection(actions, value)
}
