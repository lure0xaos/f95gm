package f95gm.server.http.response

import f95gm.domain.values.F95GameVersion
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.model.marks.MarkResponse
import f95gm.server.model.version.isNewerVersion
import f95gm.server.http.serialization.upstreamJson
import f95gm.domain.values.F95ThreadId
import f95gm.server.model.filters.SavedFilter

internal fun marksJson(
    marks: List<GameMark>,
    live: Map<F95ThreadId, LiveMarkInfo> = emptyMap()
): String = upstreamJson.encodeToString(marks.map { mark ->
    val info = live[mark.threadId] ?: LiveMarkInfo.fallback(mark.threadId)
    val latestVersion = info.version.value
    MarkResponse(
        threadId = mark.threadId,
        trackingState = mark.trackingState,
        title = info.title,
        version = info.version,
        developer = info.developer,
        cover = info.cover,
        prefixes = info.prefixes,
        date = info.date,
        likes = info.likes,
        views = info.views,
        rating = info.rating,
        isNew = info.isNew,
        storedVersion = mark.storedVersion,
        latestVersion = F95GameVersion(latestVersion),
        updateAvailable = isNewerVersion(latestVersion, mark.storedVersion.value),
        updatedAt = mark.updatedAt
    )
})

internal fun savedFiltersJson(filters: List<SavedFilter>): String =
    upstreamJson.encodeToString(filters)
