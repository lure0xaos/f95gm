package f95gm.client.data.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PayloadParserTest {
    @Test
    fun malformedJsonIsReportedAsInvalidPayload() {
        val result = parsePayload<Map<String, String>, String>("{") { it.toString() }

        assertEquals(PayloadParseResult.Failure(ParseFailureReason.INVALID_PAYLOAD), result)
    }

    @Test
    fun unexpectedMapperFailureIsNotMisreportedAsInvalidPayload() {
        assertFailsWith<IllegalStateException> {
            parsePayload<Map<String, String>, String>("{}") { error("mapper defect") }
        }
    }

    @Test
    fun explicitMappingFailureRemainsAParseFailure() {
        val result = parsePayload<Map<String, String>, String>("{}") {
            throw PayloadMappingException(ParseFailureReason.INVALID_PAYLOAD)
        }

        assertEquals(PayloadParseResult.Failure(ParseFailureReason.INVALID_PAYLOAD), result)
    }
}
