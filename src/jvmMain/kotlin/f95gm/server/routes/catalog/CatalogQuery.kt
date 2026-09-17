package f95gm.server.routes.catalog

import f95gm.domain.api.F95ApiQueryParameters
import f95gm.domain.defaults.F95Defaults
import f95gm.server.http.response.encode

@JvmInline
internal value class CatalogPage(val value: Int) {
    init { require(value > 0) { "Catalog page must be positive" } }
}

internal data class CatalogQuery(
    val category: String,
    val page: CatalogPage,
    val rows: Int,
    val sort: String,
    val cacheBuster: Long,
    val tags: List<String>,
    val excludedTags: List<String>,
    val prefixes: List<String>,
    val excludedPrefixes: List<String>,
    val tagType: String,
    val date: Int,
    val search: String,
    val creator: String
) {
    fun encoded(): String = CatalogQuerySerializer.encode(this)
}

private object CatalogQuerySerializer {
    fun encode(query: CatalogQuery): String = buildList {
        add(parameter(F95ApiQueryParameters.COMMAND.value, "list"))
        add(parameter(F95ApiQueryParameters.UPSTREAM_CATEGORY.value, query.category))
        add(parameter(F95ApiQueryParameters.PAGE.value, query.page.value.toString()))
        add(parameter(F95ApiQueryParameters.SORT.value, query.sort))
        add(parameter(F95ApiQueryParameters.ROWS.value, query.rows.toString()))
        add(parameter(F95ApiQueryParameters.CACHE_BUSTER.value, query.cacheBuster.toString()))
        query.tags.forEach { add(parameter("${F95ApiQueryParameters.TAGS.value}[]", it)) }
        query.excludedTags.forEach { add(parameter("${F95ApiQueryParameters.EXCLUDED_TAGS.value}[]", it)) }
        query.prefixes.forEach { add(parameter("${F95ApiQueryParameters.PREFIXES.value}[]", it)) }
        query.excludedPrefixes.forEach { add(parameter("${F95ApiQueryParameters.EXCLUDED_PREFIXES.value}[]", it)) }
        if (query.tags.isNotEmpty() || query.excludedTags.isNotEmpty()) {
            add(parameter(F95ApiQueryParameters.TAG_TYPE.value, query.tagType))
        }
        if (query.date > F95Defaults.ANY_DATE) add(parameter(F95ApiQueryParameters.DATE.value, query.date.toString()))
        if (query.search.isNotBlank()) add(parameter(F95ApiQueryParameters.SEARCH.value, query.search))
        if (query.creator.isNotBlank()) add(parameter(F95ApiQueryParameters.CREATOR.value, query.creator))
    }.joinToString("&")

    private fun parameter(name: String, value: String): String = "${encode(name)}=${encode(value)}"
}
