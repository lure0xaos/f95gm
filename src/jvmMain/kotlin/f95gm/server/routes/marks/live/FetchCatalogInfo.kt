package f95gm.server.routes.marks.live

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.*
import f95gm.messages.UiMessages
import f95gm.server.http.response.encode
import f95gm.server.http.response.isSuccessful
import f95gm.server.http.serialization.upstreamJson
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.routes.marks.live.MarkRouteDependencies
import f95gm.server.routes.catalog.catalogRequest
import f95gm.server.upstream.request.*
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException

internal suspend fun fetchCatalogInfo(
    context: MarkRouteDependencies,
    title: String,
    threadId: F95ThreadId,
    session: UpstreamSession
): CatalogInfoResult {
    val query = listOf(
        "cmd=list",
        "cat=games",
        "page=${F95Defaults.FIRST_PAGE}",
        "sort=date",
        "rows=${context.catalogRequestPolicy.defaultRows}",
        "_=${context.clock.epochMillis().value}",
        "search=${encode(title.take(context.catalogRequestPolicy.maxTextLength.value))}"
    ).joinToString("&")
    val path = UpstreamPaths.catalog(query, context.upstreamEndpoints)
    val result = context.upstream.request(catalogRequest(path, session, context.upstreamEndpoints))
    if (!result.status.isSuccessful) return CatalogInfoResult.UpstreamFailure(result.status)
    return mapCatalogInfoResponse(result.body, title, threadId)
}

internal fun mapCatalogInfoResponse(body: String, title: String, threadId: F95ThreadId): CatalogInfoResult {
    val payload = try {
        upstreamJson.decodeFromString<UpstreamCatalogPayload>(body)
    } catch (_: SerializationException) {
        return CatalogInfoResult.InvalidPayload
    }
    val item = payload.msg?.data?.asSequence()?.map { it.toValues() }
        ?.firstOrNull { it.threadId == threadId.value }
        ?: return CatalogInfoResult.NotFound
    return CatalogInfoResult.Found(LiveMarkInfo(
        threadId = threadId,
        title = F95GameTitle(item.title.ifBlank { title }),
        version = F95GameVersion(item.version.ifBlank { F95Defaults.UNKNOWN_VERSION }),
        developer = F95DeveloperName(item.developer),
        cover = F95CoverUrl(item.cover),
        prefixes = item.prefixes.map(::F95PrefixId),
        date = F95DateLabel(item.date.ifBlank { UiMessages.app_recently() }),
        likes = F95MetricText(item.likes.ifBlank { F95Defaults.ZERO_METRIC }),
        views = F95MetricText(item.views.ifBlank { F95Defaults.ZERO_METRIC }),
        rating = item.rating,
        isNew = item.isNew
    ))
}

internal sealed interface CatalogInfoResult {
    data class Found(val value: LiveMarkInfo) : CatalogInfoResult
    data class UpstreamFailure(val status: HttpStatusCode) : CatalogInfoResult
    data object InvalidPayload : CatalogInfoResult
    data object NotFound : CatalogInfoResult
}
