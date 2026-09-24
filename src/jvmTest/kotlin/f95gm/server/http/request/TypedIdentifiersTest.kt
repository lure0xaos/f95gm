package f95gm.server.http.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TypedIdentifiersTest {
    @Test
    fun acceptsOnlyTheDocumentedIdentifierAlphabet() {
        (0..128).forEach { length ->
            val value = "a".repeat(length)
            if (length == 0 || length > 128) {
                assertNull(validIdempotencyKey(value))
            } else {
                assertEquals(value, validIdempotencyKey(value)?.value)
            }
        }
        assertNull(validIdempotencyKey("contains space"))
        assertNull(validIdempotencyKey("contains/slash"))
    }
}
