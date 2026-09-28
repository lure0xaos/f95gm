package f95gm.client.actions.catalog.query

import f95gm.client.model.catalog.CatalogState
import f95gm.client.model.catalog.CatalogSelectors
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.domain.api.ApiPath
import f95gm.domain.api.apiQuery
import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.F95ApiQueryParameters
import f95gm.domain.filters.F95SearchMode
import kotlinx.coroutines.flow.first

internal data class CatalogQuery(
    val category: String,
    val page: Int,
    val sort: String,
    val rows: Int,
    val search: String,
    val creatorSearch: String,
    val searchMode: F95SearchMode,
    val dateLimit: Int,
    val tagType: String,
    val tags: List<String>,
    val excludedTags: List<String>,
    val prefixes: List<String>,
    val excludedPrefixes: List<String>
) {
    fun toPath(): ApiPath {
        val query = apiQuery(F95ApiPaths.CATALOG)
            .add(F95ApiQueryParameters.CATEGORY, queryEncode(category))
            .add(F95ApiQueryParameters.PAGE, page.toString())
            .add(F95ApiQueryParameters.SORT, queryEncode(sort))
            .add(F95ApiQueryParameters.ROWS, rows.toString())
        val primarySearchKey = if (searchMode == F95SearchMode.CREATOR) F95ApiQueryParameters.CREATOR else F95ApiQueryParameters.SEARCH
        if (search.isNotBlank()) query.add(primarySearchKey, queryEncode(search))
        if (creatorSearch.isNotBlank()) query.add(F95ApiQueryParameters.CREATOR, queryEncode(creatorSearch))
        if (dateLimit >= 0) query.add(F95ApiQueryParameters.DATE, dateLimit.toString())
        if (tags.isNotEmpty() || excludedTags.isNotEmpty()) query.add(F95ApiQueryParameters.TAG_TYPE, tagType)
        tags.forEach { query.add(F95ApiQueryParameters.TAGS, queryEncode(it)) }
        excludedTags.forEach { query.add(F95ApiQueryParameters.EXCLUDED_TAGS, queryEncode(it)) }
        prefixes.forEach { query.add(F95ApiQueryParameters.PREFIXES, queryEncode(it)) }
        excludedPrefixes.forEach { query.add(F95ApiQueryParameters.EXCLUDED_PREFIXES, queryEncode(it)) }
        return query.build()
    }
}

internal suspend fun catalogQuery(runtime: ClientCatalogActionsPort, state: CatalogState): CatalogQuery = CatalogQuery(
    category = state.category.wireValue,
    page = state.page,
    sort = state.sort.wireValue,
    rows = runtime.appSettingsView.data.first().pageSize.value,
    search = CatalogSelectors.search(state),
    creatorSearch = CatalogSelectors.creatorSearch(state),
    searchMode = state.searchMode,
    dateLimit = CatalogSelectors.dateLimit(state),
    tagType = state.tagType.wireValue,
    tags = state.selectedTags.map { it.id.value },
    excludedTags = state.excludedTags.map { it.id.value },
    prefixes = state.selectedPrefixes.map { it.id.value },
    excludedPrefixes = state.excludedPrefixes.map { it.id.value }
)
