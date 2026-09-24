package f95gm.server.routes.catalog

import f95gm.domain.api.F95Api
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95TagType
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.filters.filterCategories
import f95gm.server.config.filters.filterSorts
import f95gm.server.config.upstream.latestApi
import f95gm.server.config.upstream.latestPage
import f95gm.server.config.upstream.upstream
import f95gm.server.http.request.allowedTextParameter
import f95gm.server.http.request.intParameter
import f95gm.server.http.request.oneOfIntParameter
import f95gm.server.http.request.textParameter
import f95gm.server.http.response.encode
import f95gm.server.http.response.respond
import f95gm.server.http.response.respondUpstreamFailure
import f95gm.server.model.http.userFacingBody
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.routeDependencies
import f95gm.server.upstream.request.UpstreamHeaders
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import io.ktor.server.application.*

internal suspend fun catalog(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToCatalog()) ?: return
    val category = call.request.allowedTextParameter(F95Api.CATEGORY, F95Defaults.DEFAULT_CATEGORY, filterCategories)
    val page =
        call.request.intParameter(F95Api.PAGE, F95Defaults.FIRST_PAGE, F95Defaults.FIRST_PAGE..ServerConstants.MAX_PAGE)
    val rows =
        call.request.oneOfIntParameter(F95Api.ROWS, ServerConstants.API_PAGE_SIZE, ServerConstants.API_PAGE_SIZES)
    val sort = call.request.allowedTextParameter(F95Api.SORT, F95Defaults.DEFAULT_SORT, filterSorts)
    val tags = queryValues(call, F95Api.TAGS).filter(::isNumericId).distinct().take(ServerConstants.MAX_TAGS)
    val excludedTags =
        queryValues(call, F95Api.EXCLUDED_TAGS).filter(::isNumericId).distinct().take(ServerConstants.MAX_TAGS)
    val prefixes = queryValues(call, F95Api.PREFIXES).filter(::isNumericId).distinct()
    val excludedPrefixes = queryValues(call, F95Api.EXCLUDED_PREFIXES).filter(::isNumericId).distinct()
    val tagType = call.request.allowedTextParameter(
        F95Api.TAG_TYPE,
        F95Defaults.DEFAULT_TAG_TYPE,
        listOf(F95TagType.AND.wireValue, F95TagType.OR.wireValue)
    )
    val date =
        call.request.intParameter(F95Api.DATE, F95Defaults.ANY_DATE, F95Defaults.ANY_DATE..F95Defaults.MAX_DATE_DAYS)
    val search = call.request.textParameter(F95Api.SEARCH, ServerConstants.MAX_TEXT_LENGTH)
    val creator = call.request.textParameter(F95Api.CREATOR, ServerConstants.MAX_TEXT_LENGTH)
    val query = buildList {
        add("cmd=list")
        add("cat=${encode(category)}")
        add("${F95Api.PAGE}=$page")
        add("sort=${encode(sort)}")
        add("rows=$rows")
        add("_=${call.routeDependencies.clock.nowMillis()}")
        tags.forEach { add("${F95Api.TAGS}%5B%5D=${encode(it)}") }
        excludedTags.forEach { add("${F95Api.EXCLUDED_TAGS}%5B%5D=${encode(it)}") }
        prefixes.forEach { add("${F95Api.PREFIXES}%5B%5D=${encode(it)}") }
        excludedPrefixes.forEach { add("${F95Api.EXCLUDED_PREFIXES}%5B%5D=${encode(it)}") }
        if (tags.isNotEmpty() || excludedTags.isNotEmpty()) add("${F95Api.TAG_TYPE}=${encode(tagType)}")
        if (date > F95Defaults.ANY_DATE) add("${F95Api.DATE}=$date")
        if (search.isNotBlank()) add("${F95Api.SEARCH}=${encode(search)}")
        if (creator.isNotBlank()) add("${F95Api.CREATOR}=${encode(creator)}")
    }.joinToString("&")
    val path = "${latestApi.removePrefix(upstream)}?$query"
    val result = call.routeDependencies.upstreamRequest(
        method = HttpMethod.Get,
        path = path.removePrefix(upstream),
        session = session,
        headers = UpstreamHeaders.of(
            mapOf(
            HttpHeaders.Referrer to latestPage,
            "X-Requested-With" to "XMLHttpRequest",
            HttpHeaders.Accept to "application/json, text/javascript, */*; q=0.01"
            )
        )
    )
    if (!call.respondUpstreamFailure(result)) {
        respond(call, result.status, result.userFacingBody, result.contentType)
    }
}
