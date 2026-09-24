package f95gm.server.config.logging

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StructuredLogTest {
    @Test
    fun onlyAllowlistedFieldsAreEmittedAndValuesAreRedacted() {
        val line = structuredLog(
            "request_completed", listOf(
                logField("requestId", "req-1"),
                logField("status", 200),
                logField("cookie", "secret-token"),
                logField("path", "/a b")
            )
        )

        assertTrue("requestId=req-1" in line)
        assertTrue("path=/a\\sb" in line)
        assertFalse("cookie" in line)
        assertFalse("secret-token" in line)
    }

    @Test
    fun typedRequestEventUsesTheStableSchema() {
        val line = structuredLog(RequestCompletedEvent("req-2", "GET", "/api/health", 200, 4L, "success", null))

        assertTrue(line.startsWith("event=request_completed"))
        assertTrue("requestId=req-2" in line)
        assertTrue("durationMs=4" in line)
        assertTrue("outcome=success" in line)
    }
}
