package f95gm.server.routes.marks.live

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.*

class JsonObjectValuesTest {
    private val values = JsonObject(
        mapOf(
            "title" to JsonPrimitive("Game"),
            "score" to JsonPrimitive(8.5),
            "enabled" to JsonPrimitive(true),
            "nested" to JsonObject(mapOf("id" to JsonPrimitive("42"))),
            "null" to JsonNull
        )
    )

    @Test
    fun extractsTypedValuesAndNestedObjects() {
        assertEquals("Game", values.string("title"))
        assertEquals(8.5, values.double("score"))
        assertTrue(values.boolean("enabled"))
        assertEquals(JsonObject(mapOf("id" to JsonPrimitive("42"))), values.jsonObject("nested"))
    }

    @Test
    fun appliesFallbacksForMissingNullAndInvalidPrimitiveValues() {
        assertEquals("fallback", values.string("missing", "fallback"))
        assertEquals("", values.string("null"))
        assertEquals(0.0, values.double("missing"))
        assertEquals(0.0, JsonObject(mapOf("score" to JsonPrimitive("not-a-number"))).double("score"))
        assertFalse(values.boolean("missing"))
        assertNull(values.jsonObject("title"))
    }
}
