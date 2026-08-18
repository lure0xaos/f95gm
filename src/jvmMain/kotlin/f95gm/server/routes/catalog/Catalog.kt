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
import f95gm.server.http.response.encode
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.routes.session.browserSession
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import io.ktor.server.application.*

internal suspend fun catalog(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToCatalog()))
        return
    }
    val category = call.request.queryParameters[F95Api.CATEGORY]
        ?.takeIf { it in filterCategories } ?: F95Defaults.DEFAULT_CATEGORY
    val page = call.request.queryParameters[F95Api.PAGE]?.toIntOrNull()
        ?.coerceIn(F95Defaults.FIRST_PAGE, ServerConstants.MAX_PAGE) ?: F95Defaults.FIRST_PAGE
    val rows = call.request.queryParameters[F95Api.ROWS]?.toIntOrNull()
        ?.takeIf { it in ServerConstants.API_PAGE_SIZES } ?: ServerConstants.API_PAGE_SIZE
    val sort = call.request.queryParameters[F95Api.SORT]
        ?.takeIf { it in filterSorts } ?: F95Defaults.DEFAULT_SORT
    val tags = queryValues(call, F95Api.TAGS).filter(::isNumericId).distinct().take(ServerConstants.MAX_TAGS)
    val excludedTags =
        queryValues(call, F95Api.EXCLUDED_TAGS).filter(::isNumericId).distinct().take(ServerConstants.MAX_TAGS)
    val prefixes = queryValues(call, F95Api.PREFIXES).filter(::isNumericId).distinct()
    val excludedPrefixes = queryValues(call, F95Api.EXCLUDED_PREFIXES).filter(::isNumericId).distinct()
    val tagType = call.request.queryParameters[F95Api.TAG_TYPE]
        ?.takeIf { it == F95TagType.AND.wireValue || it == F95TagType.OR.wireValue } ?: F95Defaults.DEFAULT_TAG_TYPE
    val date = call.request.queryParameters[F95Api.DATE]?.toIntOrNull()
        ?.coerceIn(F95Defaults.ANY_DATE, F95Defaults.MAX_DATE_DAYS) ?: F95Defaults.ANY_DATE
    val search = call.request.queryParameters[F95Api.SEARCH]?.trim()?.take(ServerConstants.MAX_TEXT_LENGTH).orEmpty()
    val creator = call.request.queryParameters[F95Api.CREATOR]?.trim()?.take(ServerConstants.MAX_TEXT_LENGTH).orEmpty()
    val query = buildList {
        add("cmd=list")
        add("cat=${encode(category)}")
        add("${F95Api.PAGE}=$page")
        add("sort=${encode(sort)}")
        add("rows=$rows")
        add("_=${System.currentTimeMillis()}")
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
    val result = upstreamRequest(
        method = "GET",
        path = path.removePrefix(upstream),
        session = session,
        headers = mapOf(
            HttpHeaders.Referrer to latestPage,
            "X-Requested-With" to "XMLHttpRequest",
            HttpHeaders.Accept to "application/json, text/javascript, */*; q=0.01"
        )
    )
    respond(call, result.status, result.body, result.contentType)
}
