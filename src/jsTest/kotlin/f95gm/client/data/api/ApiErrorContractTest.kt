package f95gm.client.data.api

import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApiErrorContractTest {
    @Test
    fun serverErrorEnvelopeUsesTheSharedMessageField() {
        val encoded = Json.encodeToString(F95ApiError("invalid request"))

        assertEquals("invalid request", readApiError(encoded, "fallback"))
    }

    @Test
    fun malformedJsonUsesTheFallbackWithoutThrowing() {
        listOf("", "{", "[]", "{\"message\":", "not-json").forEach { malformed ->
            assertEquals("fallback", readApiError(malformed, "fallback"))
        }
        assertNotNull(readApiError(Json.encodeToString(F95ApiError("bad", F95ApiErrorCode.INVALID_INPUT)), "fallback"))
    }

    @Test
    fun errorEnvelopeCarriesLocalizationKeyAndFieldDetails() {
        val decoded = Json.decodeFromString<F95ApiError>(
            "{\"message\":\"bad\",\"code\":\"INVALID_INPUT\",\"messageKey\":\"server.invalidJsonRequest\",\"details\":{\"field\":\"name\"}}"
        )
        assertEquals(F95ApiMessageKey("server.invalidJsonRequest"), decoded.messageKey)
        assertEquals("name", decoded.details["field"])
        assertEquals("name", decoded.typedDetails.field)
    }

    @Test
    fun preservesUnknownErrorDetailsAsTypedExtensions() {
        val decoded = Json.decodeFromString<F95ApiError>(
            "{\"message\":\"bad\",\"details\":{\"traceId\":\"abc\",\"field\":\"name\"}}"
        )

        assertEquals("abc", decoded.typedDetails.extensions["traceId"])
        assertEquals("name", decoded.typedDetails.field)
    }
}
