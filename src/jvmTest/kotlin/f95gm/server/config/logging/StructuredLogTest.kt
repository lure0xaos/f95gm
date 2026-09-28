package f95gm.server.observability

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StructuredLogTest {
    @Test
    fun redactsSensitiveValuesAndNormalizesControlCharacters() {
        val value = f95gm.server.http.logging.redactLogValue("token=secret\npassword='hidden'")
        assertTrue("<redacted>" in value)
        assertFalse("secret" in value)
        assertFalse("hidden" in value)
        assertFalse("\n" in value)
    }
    @Test
    fun onlyAllowlistedFieldsAreEmittedAndValuesAreRedacted() {
        val line = structuredLog(
            "request_completed", listOf(
                logField(StructuredLogKeys.REQUEST_ID, "req-1"),
                logField(StructuredLogKeys.STATUS, 200),
                logField(StructuredFieldName("cookie"), "secret-token"),
                logField(StructuredLogKeys.PATH, "/a b")
            )
        )

        assertTrue("requestId=req-1" in line)
        assertTrue("path=/a\\sb" in line)
        assertFalse("cookie" in line)
        assertFalse("secret-token" in line)
    }

    @Test
    fun typedRequestEventUsesTheStableSchema() {
        val line = structuredLog(RequestCompletedEvent("req-2", "GET", "/api/health", "200", 4L, RequestOutcome.SUCCESS, null))

        assertTrue(line.startsWith("event=request_completed"))
        assertTrue("requestId=req-2" in line)
        assertTrue("durationMs=4" in line)
        assertTrue("outcome=success" in line)
    }

    @Test
    fun structuredLogRenderingUsesAnInjectableSink() {
        val event = RequestCompletedEvent("req-sink", "GET", "/health", "200", 1L, RequestOutcome.SUCCESS, null)
        val rendered = structuredLog(event, StructuredLogSink { "captured=${it.name.value}" })

        assertEquals("captured=request_completed correlationId=req-sink", rendered)
    }

    @Test
    fun everyStructuredEventDeclaresItsRenderedFields() {
        assertEquals(StructuredLogEventRegistry.names, StructuredLogEventRegistry.names.filter { allowedStructuredFields(it).isNotEmpty() }.toSet())
    }

    @Test
    fun undeclaredEventFieldsAreRejectedInsteadOfLeakingIntoLogs() {
        val line = structuredLog("request_completed", listOf(logField(StructuredFieldName("undocumented"), "value")))
        assertFalse("undocumented" in line)
    }

    @Test
    fun typedEventsRejectDuplicateFields() {
        assertFailsWith<IllegalArgumentException> {
            StructuredLogSchema.validate(
                StructuredEventName("request_completed"),
                listOf(logField(StructuredLogKeys.REQUEST_ID, "one"), logField(StructuredLogKeys.REQUEST_ID, "two"))
            )
        }
    }

    @Test
    fun registryMatchesEveryRenderedSchema() {
        assertEquals(
            StructuredLogEventRegistry.names,
            StructuredLogEventRegistry.names.filter { allowedStructuredFields(it).isNotEmpty() }.toSet()
        )
    }
}
