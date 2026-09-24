package f95gm.server.http.response

import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiFailureDetailsTest {
    @Test
    fun combinesFieldAndExtraDetailsWithoutDroppingEither() {
        val details = ApiFailureDetails(fieldName = "name", reason = "invalid")

        assertEquals(mapOf("field" to "name", "reason" to "invalid"), details.asMap())
        assertEquals(mapOf("field" to "threadId"), ApiFailureDetails.field("threadId").asMap())
    }

    @Test
    fun jsonMessagesCarryMachineReadableErrorFields() {
        val encoded = jsonMessage(
            message = "Bad request",
            messageKey = ServerMessageKeys.INVALID_JSON_REQUEST.apiKey,
            code = F95ApiErrorCode.INVALID_INPUT,
            details = mapOf("field" to "body")
        )
        val decoded = Json.decodeFromString<F95ApiError>(encoded)

        assertEquals("Bad request", decoded.message)
        assertEquals(F95ApiErrorCode.INVALID_INPUT, decoded.code)
        assertEquals("server.invalidJsonRequest", decoded.messageKey?.value)
        assertEquals("body", decoded.details["field"])
    }

    @Test
    fun supportsTypedReasonAndStatusDetails() {
        assertEquals(
            mapOf("reason" to "timeout", "status" to "504"),
            ApiFailureDetails(reason = "timeout", status = 504).asMap()
        )
    }
}
