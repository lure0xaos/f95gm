package f95gm.server.http.request

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class RequestIdentifiersTest {
    @Test
    fun typedIdentifiersEnforceTheirDifferentContracts() {
        assertEquals("tab-1", BrowserTabId("tab-1").value)
        assertEquals("request_1", RequestId("request_1").value)
        assertEquals("token_123", ShutdownToken("token_123").value)
        assertEquals("route", RequestEndpoint("route").value)

        assertFailsWith<IllegalArgumentException> { BrowserTabId("tab.id") }
        assertFailsWith<IllegalArgumentException> { RequestId("request.id") }
        assertFailsWith<IllegalArgumentException> { ShutdownToken("short") }
        assertFailsWith<IllegalArgumentException> { RequestEndpoint(" ") }
    }

    @Test
    fun secureGeneratorsProduceValidNonEmptyIdentifiers() {
        val firstRequestId = SecureRequestIdentifiers.requestIds.next()
        val secondRequestId = SecureRequestIdentifiers.requestIds.next()
        val shutdownToken = SecureRequestIdentifiers.shutdownTokens.next()

        assertNotEquals(firstRequestId, secondRequestId)
        assertEquals(36, firstRequestId.value.length)
        assertEquals(36, shutdownToken.value.length)
    }

    @Test
    fun identifierFactoryAllowsDeterministicUuidInjection() {
        val factory = RequestIdentifierFactory { UUID.fromString("00000000-0000-0000-0000-000000000001") }

        assertEquals("00000000-0000-0000-0000-000000000001", factory.requestIds.next().value)
        assertEquals("00000000-0000-0000-0000-000000000001", factory.shutdownTokens.next().value)
    }

    @Test
    fun shutdownTokensAcceptBoundaryLengthsAndRejectUnsafeCharacters() {
        assertEquals(8, ShutdownToken("12345678").value.length)
        assertEquals(128, ShutdownToken("a".repeat(128)).value.length)
        assertFailsWith<IllegalArgumentException> { ShutdownToken("1234567") }
        assertFailsWith<IllegalArgumentException> { ShutdownToken("a".repeat(129)) }
        assertFailsWith<IllegalArgumentException> { ShutdownToken("token=value") }
        assertFailsWith<IllegalArgumentException> { ShutdownToken("token value") }
    }
}
