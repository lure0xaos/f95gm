package f95gm.client.data.json

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonPrimitiveHelpersTest {
    private val objectValue = JsonObject(
        mapOf(
            "text" to JsonPrimitive("value"),
            "number" to JsonPrimitive(42),
            "invalid" to JsonPrimitive("not-a-number"),
            "null" to JsonNull
        )
    )

    @Test
    fun readsStringsAndIntegers() {
        assertEquals("value", objectValue.string("text"))
        assertEquals(42, objectValue.int("number", -1))
    }

    @Test
    fun usesFallbacksForMissingNullAndInvalidValues() {
        assertEquals("fallback", objectValue.string("missing", "fallback"))
        assertEquals("fallback", objectValue.string("null", "fallback"))
        assertEquals(-1, objectValue.int("missing", -1))
        assertEquals(-1, objectValue.int("invalid", -1))
    }
}
