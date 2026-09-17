package f95gm.server.http.request

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95MarkRequest
import f95gm.server.http.response.ServerMessageKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RequestValidationTest {
    @Test
    fun decodesValidJsonAndPreservesDefaults() {
        val decoded =
            assertIs<RequestValidation.Valid<F95MarkRequest>>(decodeJson<F95MarkRequest>("{\"acknowledge\":true}"))

        assertEquals(true, decoded.value.acknowledge)
        assertEquals(null, decoded.value.trackingState)
    }

    @Test
    fun turnsMalformedJsonIntoAStableInvalidRequest() {
        val invalid = assertIs<RequestValidation.Invalid>(decodeJson<F95MarkRequest>("{"))

        assertEquals(F95ApiErrorCode.INVALID_INPUT, invalid.code)
        assertEquals("Invalid JSON request", invalid.message)
        assertEquals(ServerMessageKey.INVALID_JSON_REQUEST, invalid.messageKey)
        assertEquals(null, invalid.field)
    }

    @Test
    fun mapsFieldValidationDetailsToAnApiFailure() {
        val invalid = RequestValidation.Invalid.field(
            F95ApiErrorCode.INVALID_INPUT,
            "bad value",
            ServerMessageKey.INVALID_TRACKING_STATE,
            "trackingState"
        )

        val failure = invalid.toApiFailure()

        assertEquals("bad value", failure.message)
        assertEquals(ServerMessageKey.INVALID_TRACKING_STATE, failure.key)
        assertEquals(mapOf("field" to "trackingState"), failure.details.asMap())
    }
}

