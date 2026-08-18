package f95gm.server.model.marks

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.*
import f95gm.messages.UiMessages

internal data class LiveMarkInfo(
    val threadId: F95ThreadId,
    val title: F95GameTitle,
    val version: F95GameVersion,
    val developer: F95DeveloperName,
    val cover: F95CoverUrl,
    val prefixes: List<F95PrefixId>,
    val date: F95DateLabel,
    val likes: F95MetricText,
    val views: F95MetricText,
    val rating: Double,
    val isNew: Boolean
) {
    companion object {
        fun fallback(threadId: F95ThreadId): LiveMarkInfo = LiveMarkInfo(
            threadId = threadId,
            title = F95GameTitle(UiMessages.app_untitledGame()),
            version = F95GameVersion(F95Defaults.UNKNOWN_VERSION),
            developer = F95DeveloperName(F95Defaults.EMPTY),
            cover = F95CoverUrl(F95Defaults.EMPTY),
            prefixes = emptyList(),
            date = F95DateLabel(UiMessages.app_recently()),
            likes = F95MetricText(F95Defaults.ZERO_METRIC),
            views = F95MetricText(F95Defaults.ZERO_METRIC),
            rating = 0.0,
            isNew = false
        )
    }
}
