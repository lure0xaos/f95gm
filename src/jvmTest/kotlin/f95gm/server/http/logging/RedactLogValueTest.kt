package f95gm.server.http.logging

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RedactLogValueTest {
    @Test
    fun removesCredentialsAndSessionValuesFromDiagnosticText() {
        val redacted = redactLogValue("password=hunter2 token=abc cookie=session authorization=Bearer-secret")
        assertTrue(redacted.contains("password=<redacted>"))
        assertFalse(redacted.contains("hunter2"))
        assertFalse(redacted.contains("Bearer-secret"))
    }

    @Test
    fun redactsStructuredAndCaseInsensitiveValues() {
        val redacted = redactLogValue("Authorization: \"Bearer top-secret\" TOKEN=xyz? password='quoted-secret'")
        assertTrue(redacted.contains("Authorization: <redacted>"))
        assertTrue(redacted.contains("TOKEN=<redacted>"))
        assertTrue(redacted.contains("password=<redacted>"))
        assertFalse(redacted.contains("top-secret"))
        assertFalse(redacted.contains("quoted-secret"))
    }

    @Test
    fun redactsAccountAndSessionIdentifiers() {
        val redacted = redactLogValue("accountKey=user@example.test browserId=browser-1 sessionId=session-1")
        assertTrue(redacted.contains("accountKey=<redacted>"))
        assertFalse(redacted.contains("user@example.test"))
        assertFalse(redacted.contains("browser-1"))
        assertFalse(redacted.contains("session-1"))
    }
}
