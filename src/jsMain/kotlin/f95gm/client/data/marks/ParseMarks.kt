package f95gm.client.data.marks

import f95gm.client.data.json.string
import f95gm.client.model.marks.MarkedGame
import f95gm.domain.marks.MarkJsonKeys
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.*
import f95gm.messages.UiMessages
import kotlinx.serialization.json.*

internal fun parseMarks(payload: String): List<MarkedGame> = Json.parseToJsonElement(payload).jsonArray.map { element ->
    val item = element.jsonObject
    MarkedGame(
        threadId = F95ThreadId(item.string("threadId")),
        trackingState = GameTrackingState.fromWire(
            item.string(
                MarkJsonKeys.TRACKING_STATE,
                GameTrackingState.WATCHING.wireValue
            )
        )
            ?: GameTrackingState.WATCHING,
        title = F95GameTitle(item.string("title", UiMessages.app_untitledGame())),
        version = F95GameVersion(item.string("version", UiMessages.app_versionUnknown())),
        developer = F95DeveloperName(item.string("developer")),
        cover = F95CoverUrl(item.string("cover")),
        prefixes = item["prefixes"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.map(::F95PrefixId)
            ?.takeIf { it.isNotEmpty() }
            .orEmpty(),
        date = F95DateLabel(item.string("date", UiMessages.app_recently())),
        likes = F95MetricText(item.string("likes", "0")),
        views = F95MetricText(item.string("views", "0")),
        rating = item["rating"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
        isNew = item["isNew"]?.jsonPrimitive?.booleanOrNull == true,
        storedVersion = StoredVersion(item.string("storedVersion", UiMessages.app_versionUnknown())),
        latestVersion = F95GameVersion(
            item.string(
                "latestVersion",
                item.string("version", UiMessages.app_versionUnknown())
            )
        ),
        updateAvailable = item["updateAvailable"]?.jsonPrimitive?.booleanOrNull == true,
        updatedAt = item["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L
    )
}
