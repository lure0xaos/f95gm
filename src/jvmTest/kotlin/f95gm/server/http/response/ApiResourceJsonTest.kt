package f95gm.server.http.response

import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.server.model.EpochMillis
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiResourceJsonTest {
    @Test
    fun markResponseUsesLiveMetadataAndReportsVersionAvailability() {
        val threadId = F95ThreadId("42")
        val mark = GameMark(
            threadId = threadId,
            trackingState = GameTrackingState.DOWNLOADED,
            storedVersion = StoredVersion("1.2"),
            updatedAt = EpochMillis(123L)
        )
        val live = LiveMarkInfo.fallback(threadId).copy(
            title = f95gm.domain.values.F95GameTitle("A title"),
            version = f95gm.domain.values.F95GameVersion("1.3"),
            isNew = true
        )

        val response = Json.parseToJsonElement(marksJson(listOf(mark), mapOf(threadId to live)))
            .jsonArray.single().jsonObject

        assertEquals("42", response.getValue("threadId").jsonPrimitive.content)
        assertEquals("A title", response.getValue("title").jsonPrimitive.content)
        assertEquals("1.3", response.getValue("latestVersion").jsonPrimitive.content)
        assertEquals("true", response.getValue("updateAvailable").jsonPrimitive.content)
        assertEquals("123", response.getValue("updatedAt").jsonPrimitive.content)
    }

    @Test
    fun missingLiveMetadataFallsBackAndEmptyListsSerializeAsArrays() {
        val json = Json.parseToJsonElement(marksJson(listOf(GameMark(threadId = F95ThreadId("7")))))
            .jsonArray.single().jsonObject
        assertEquals("Unknown", json.getValue("latestVersion").jsonPrimitive.content)
        assertEquals("false", json.getValue("updateAvailable").jsonPrimitive.content)
        assertEquals("[]", marksJson(emptyList()))
        assertEquals("[]", savedFiltersJson(emptyList()))
    }
}
