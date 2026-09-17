package f95gm.server.routes.catalog

import f95gm.domain.api.F95ApiQueryParameters
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95TagType
import f95gm.server.config.filters.filterCategories
import f95gm.server.config.filters.filterSorts
import f95gm.server.http.request.allowedTextParameter
import f95gm.server.http.request.intParameter
import f95gm.server.http.request.oneOfIntParameter
import f95gm.server.http.request.textParameter
import f95gm.server.routes.ports.CatalogRequestPolicy
import io.ktor.server.application.ApplicationCall

internal data class CatalogRequest(
    val category: String,
    val page: CatalogPage,
    val rows: Int,
    val sort: String,
    val tags: List<String>,
    val excludedTags: List<String>,
    val prefixes: List<String>,
    val excludedPrefixes: List<String>,
    val tagType: String,
    val date: Int,
    val search: String,
    val creator: String
)

internal fun ApplicationCall.catalogRequest(policy: CatalogRequestPolicy): CatalogRequest {
    val request = request
    return CatalogRequest(
        category = request.allowedTextParameter(F95ApiQueryParameters.CATEGORY, F95Defaults.DEFAULT_CATEGORY, filterCategories),
        page = CatalogPage(request.intParameter(F95ApiQueryParameters.PAGE, F95Defaults.FIRST_PAGE, F95Defaults.FIRST_PAGE..policy.maxPage.value)),
        rows = request.oneOfIntParameter(F95ApiQueryParameters.ROWS, policy.defaultRows, policy.allowedRows),
        sort = request.allowedTextParameter(F95ApiQueryParameters.SORT, F95Defaults.DEFAULT_SORT, filterSorts),
        tags = queryValues(this, F95ApiQueryParameters.TAGS).filter(::isNumericId).distinct().take(policy.maxTags.value),
        excludedTags = queryValues(this, F95ApiQueryParameters.EXCLUDED_TAGS).filter(::isNumericId).distinct()
            .take(policy.maxTags.value),
        prefixes = queryValues(this, F95ApiQueryParameters.PREFIXES).filter(::isNumericId).distinct(),
        excludedPrefixes = queryValues(this, F95ApiQueryParameters.EXCLUDED_PREFIXES).filter(::isNumericId).distinct(),
        tagType = request.allowedTextParameter(
            F95ApiQueryParameters.TAG_TYPE,
            F95Defaults.DEFAULT_TAG_TYPE,
            listOf(F95TagType.AND.wireValue, F95TagType.OR.wireValue)
        ),
        date = request.intParameter(
            F95ApiQueryParameters.DATE,
            F95Defaults.ANY_DATE,
            F95Defaults.ANY_DATE..F95Defaults.MAX_DATE_DAYS
        ),
        search = request.textParameter(F95ApiQueryParameters.SEARCH, policy.maxTextLength.value),
        creator = request.textParameter(F95ApiQueryParameters.CREATOR, policy.maxTextLength.value)
    )
}
