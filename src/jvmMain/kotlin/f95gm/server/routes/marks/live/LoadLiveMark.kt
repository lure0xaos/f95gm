package f95gm.server.routes.marks.live

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.F95GameVersion
import f95gm.domain.values.F95MetricText
import f95gm.domain.values.F95ThreadId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.state.runtime.RouteDependencies

internal suspend fun loadLiveMark(
    context: RouteDependencies,
    threadId: F95ThreadId,
    session: UpstreamSession
): LiveMarkInfo {
    return context.liveMarkCache.getOrPutSuspending(threadId.value) {
        loadLiveMarkUncached(context, threadId, session)
    }
}

internal fun invalidateLiveMark(context: RouteDependencies, threadId: F95ThreadId) {
    context.liveMarkCache.invalidate(threadId.value)
}

private suspend fun loadLiveMarkUncached(
    context: RouteDependencies,
    threadId: F95ThreadId,
    session: UpstreamSession
): LiveMarkInfo {
    val detail = fetchThreadDetail(context, threadId, session)
    if (detail == null) return LiveMarkInfo.fallback(threadId)

    return fetchCatalogInfo(context, detail.title.value, threadId, session)
        ?: LiveMarkInfo(
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
