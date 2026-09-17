package f95gm.client.data.marks

import f95gm.client.model.marks.MarkedGame
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.*
import f95gm.messages.UiMessages
import f95gm.client.data.api.MarkPayload
import f95gm.client.data.api.parsePayloadOrThrow

internal fun parseMarks(payload: String): List<MarkedGame> = parsePayloadOrThrow<List<MarkPayload>, List<MarkedGame>>(payload) { marks -> marks.map { item ->
    MarkedGame(
        threadId = F95ThreadId(item.threadId),
        trackingState = GameTrackingState.fromWire(
            item.trackingState
        )
            ?: GameTrackingState.WATCHING,
        title = F95GameTitle(item.title.ifBlank { UiMessages.app_untitledGame() }),
        version = F95GameVersion(item.version.ifBlank { UiMessages.app_versionUnknown() }),
        developer = F95DeveloperName(item.creator ?: item.developer.orEmpty()),
        cover = F95CoverUrl(item.cover),
        prefixes = item.prefixes
            .map(::F95PrefixId)
            .takeIf { it.isNotEmpty() }
            .orEmpty(),
        date = F95DateLabel(item.date.ifBlank { UiMessages.app_recently() }),
        likes = F95MetricText(item.likes),
        views = F95MetricText(item.views),
        rating = item.rating,
        isNew = item.isNew,
        storedVersion = StoredVersion(item.storedVersion.ifBlank { UiMessages.app_versionUnknown() }),
        latestVersion = F95GameVersion(
            item.latestVersion?.takeIf { it.isNotBlank() }
                ?: item.version.takeIf { it.isNotBlank() }
                ?: UiMessages.app_versionUnknown()
        ),
        updateAvailable = item.updateAvailable,
        updatedAt = F95EpochMillis(item.updatedAt)
    )
} }
