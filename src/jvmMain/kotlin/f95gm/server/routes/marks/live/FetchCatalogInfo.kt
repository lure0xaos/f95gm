package f95gm.server.routes.marks.live

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.*
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.upstream.latestApi
import f95gm.server.config.upstream.latestPage
import f95gm.server.config.upstream.upstream
import f95gm.server.http.response.encode
import f95gm.server.http.response.isSuccessful
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.state.runtime.RouteDependencies
import f95gm.server.upstream.request.UpstreamHeaders
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import kotlinx.serialization.json.*

internal suspend fun fetchCatalogInfo(
    context: RouteDependencies,
    title: String,
    threadId: F95ThreadId,
    session: UpstreamSession
): LiveMarkInfo? {
    val query = listOf(
        "cmd=list",
        "cat=games",
        "page=${F95Defaults.FIRST_PAGE}",
        "sort=date",
        "rows=${ServerConstants.API_PAGE_SIZE}",
        "_=${context.clock.nowMillis()}",
        "search=${encode(title.take(ServerConstants.MAX_TEXT_LENGTH))}"
    ).joinToString("&")
    val result = context.upstreamRequest(
        method = HttpMethod.Get,
        path = "${latestApi.removePrefix(upstream)}?$query",
        session = session,
        headers = UpstreamHeaders.of(
            mapOf(
            HttpHeaders.Referrer to latestPage,
            "X-Requested-With" to "XMLHttpRequest",
            HttpHeaders.Accept to "application/json, text/javascript, */*; q=0.01"
            )
        )
    )
    if (!result.status.isSuccessful) return null
    val data = runCatching {
        Json.parseToJsonElement(result.body).jsonObject["msg"]
            ?.jsonObject?.get("data")?.jsonArray.orEmpty()
    }.getOrNull() ?: return null
    val item = data.firstOrNull { it.jsonObject.string("thread_id") == threadId.value }?.jsonObject ?: return null
    return LiveMarkInfo(
        threadId = threadId,
        title = F95GameTitle(item.string("title", title)),
        version = F95GameVersion(item.string("version", F95Defaults.UNKNOWN_VERSION)),
        developer = F95DeveloperName(item.string("creator").ifBlank { item.string("developer") }),
        cover = F95CoverUrl(item.string("cover").ifBlank { item.jsonObject("images")?.string("cover").orEmpty() }),
        prefixes = item["prefixes"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty().map(::F95PrefixId),
        date = F95DateLabel(item.string("date", UiMessages.app_recently())),
        likes = F95MetricText(item.string("likes", F95Defaults.ZERO_METRIC)),
        views = F95MetricText(item.string("views", F95Defaults.ZERO_METRIC)),
        rating = item.double("rating"),
        isNew = item.boolean("new")
    )
}
