package f95gm.server.routes.marks.live

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.F95GameVersion
import f95gm.domain.values.F95MetricText
import f95gm.domain.values.F95ThreadId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.routes.marks.live.MarkRouteDependencies
import f95gm.server.model.ParseResult

internal suspend fun loadLiveMark(
    context: MarkRouteDependencies,
    threadId: F95ThreadId,
    session: UpstreamSession
): LiveMarkInfo {
    return context.liveMarkCache.getOrPutSuspending(threadId) {
        loadLiveMarkUncached(context, threadId, session)
    }
}

internal fun invalidateLiveMark(context: MarkRouteDependencies, threadId: F95ThreadId) {
    context.liveMarkCache.invalidate(threadId)
}

private suspend fun loadLiveMarkUncached(
    context: MarkRouteDependencies,
    threadId: F95ThreadId,
    session: UpstreamSession
): LiveMarkInfo {
    val detail = when (val result = fetchThreadDetail(context, threadId, session)) {
        is ParseResult.Success -> result.value
        is ParseResult.Invalid -> return LiveMarkInfo.fallback(threadId)
    }

    return when (val catalog = fetchCatalogInfo(context, detail.title.value, threadId, session)) {
        is CatalogInfoResult.Found -> catalog.value
        is CatalogInfoResult.UpstreamFailure,
        CatalogInfoResult.InvalidPayload,
        CatalogInfoResult.NotFound -> LiveMarkInfo(
            threadId = threadId,
            title = detail.title,
            version = detail.meta.firstOrNull { it.label.value.equals("Version", ignoreCase = true) }
                ?.value?.value?.takeIf { it.isNotBlank() }
                ?.let(::F95GameVersion) ?: F95GameVersion(F95Defaults.UNKNOWN_VERSION),
            developer = detail.creator,
            cover = detail.cover,
            prefixes = emptyList(),
            date = detail.date,
            likes = F95MetricText(F95Defaults.ZERO_METRIC),
            views = F95MetricText(F95Defaults.ZERO_METRIC),
            rating = 0.0,
            isNew = false
        )
    }
}
