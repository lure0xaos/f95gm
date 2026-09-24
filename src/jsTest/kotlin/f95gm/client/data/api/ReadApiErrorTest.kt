package f95gm.client.data.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadApiErrorTest {
    @Test
    fun readsStructuredErrorMetadataAndDetails() {
        val parsed = parseApiError(
            """
                {
                  "message": "Filter name is already in use",
                  "code": "INVALID_INPUT",
                  "messageKey": "server.validationFailed",
                  "details": {"field": "alias", "reason": "duplicate"}
                }
            """.trimIndent(),
            fallback = "fallback"
        )

        assertEquals("Filter name is already in use", parsed.message)
        assertEquals("INVALID_INPUT", parsed.code?.name)
        assertEquals("server.validationFailed", parsed.messageKey)
        assertEquals(mapOf("field" to "alias", "reason" to "duplicate"), parsed.details)
    }

    @Test
    fun fallsBackForMalformedOrIncompletePayloads() {
        val malformed = parseApiError("not json", fallback = "Request failed")
        assertEquals("Request failed", malformed.message)
        assertNull(malformed.code)
        assertNull(malformed.messageKey)
        assertEquals(emptyMap(), malformed.details)

        val incomplete = parseApiError("{\"code\":\"INTERNAL\"}", fallback = "Request failed")
        assertEquals("Request failed", incomplete.message)
    }
}
