package f95gm.client.model.catalog

import f95gm.domain.values.*

internal data class Game(
    val threadId: F95ThreadId,
    val title: F95GameTitle,
    val version: F95GameVersion,
    val developer: F95DeveloperName,
    val cover: F95CoverUrl,
    val tags: List<F95TagId>,
    val prefixes: List<F95PrefixId>,
    val date: F95DateLabel,
    val likes: F95MetricText,
    val views: F95MetricText,
    val rating: Double,
    val isNew: Boolean
)
