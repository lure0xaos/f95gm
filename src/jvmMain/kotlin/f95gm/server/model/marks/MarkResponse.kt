package f95gm.server.model.marks

import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.*
import kotlinx.serialization.Serializable

@Serializable
internal data class MarkResponse(
    val threadId: F95ThreadId,
    val trackingState: GameTrackingState,
    val title: F95GameTitle,
    val version: F95GameVersion,
    val developer: F95DeveloperName,
    val cover: F95CoverUrl,
    val prefixes: List<F95PrefixId>,
    val date: F95DateLabel,
    val likes: F95MetricText,
    val views: F95MetricText,
    val rating: Double,
    val isNew: Boolean,
    val storedVersion: StoredVersion,
    val latestVersion: F95GameVersion,
    val updateAvailable: Boolean,
    val updatedAt: Long
)
