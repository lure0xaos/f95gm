package f95gm.server.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EpochMillisTest {
    @Test
    fun ordersNonNegativeEpochValues() {
        assertEquals(-1, EpochMillis(1).compareTo(EpochMillis(2)))
        assertEquals(0, EpochMillis(7).compareTo(EpochMillis(7)))
    }

    @Test
    fun rejectsNegativeValues() {
        assertFailsWith<IllegalArgumentException> { EpochMillis(-1) }
    }

    @Test
    fun keepsTheWireRepresentationAsAJsonNumber() {
        val encoded = Json.encodeToString(EpochMillis.serializer(), EpochMillis(42))
        assertEquals("42", encoded)
        assertEquals(EpochMillis(42), Json.decodeFromString(EpochMillis.serializer(), encoded))
    }
}
