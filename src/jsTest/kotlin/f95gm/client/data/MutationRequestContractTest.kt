package f95gm.client.data

import f95gm.domain.api.F95MarkRequest
import f95gm.domain.api.SavedFilterRequest
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95EpochMillis
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.StoredVersion
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MutationRequestContractTest {
    @Test
    fun markRequestsRoundTripWithTrackingAndConcurrencyFields() {
        val request = F95MarkRequest(
            trackingState = GameTrackingState.WATCHING,
            storedVersion = StoredVersion("1.2"),
            acknowledge = true,
            expectedUpdatedAt = F95EpochMillis(42)
        )

        val decoded = Json.decodeFromString<F95MarkRequest>(Json.encodeToString(request))

        assertEquals(request, decoded)
    }

    @Test
    fun savedFilterRequestsRoundTripWithDefaultsAndExplicitFields() {
        val request = SavedFilterRequest(
            id = F95FilterId("favorites"),
            name = F95OptionName("My games"),
            search = "demo",
            expectedUpdatedAt = F95EpochMillis(99)
        )

        val decoded = Json.decodeFromString<SavedFilterRequest>(Json.encodeToString(request))

        assertEquals(request, decoded)
        assertEquals("favorites", decoded.id.value)
        assertEquals("My games", decoded.name.value)
    }
}
