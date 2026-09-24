package f95gm.client.data.api

import f95gm.domain.api.F95ApiErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiUserMessageTest {
    @Test
    fun mapsKnownServerMessageKeysToLocalizedUserText() {
        assertEquals(
            "Sign in is required.",
            ApiException("/api/catalog", message = "ignored", messageKey = "server.authenticationRequired")
                .apiUserMessage("fallback")
        )
        assertEquals(
            "F95zone login failed (401).",
            ApiException("/api/login", status = 401, message = "ignored", messageKey = "server.loginFailed")
                .apiUserMessage("fallback")
        )
        assertEquals(
            "F95zone media returned 415.",
            ApiException(
                "/api/media",
                status = 502,
                message = "ignored",
                messageKey = "server.mediaStatus",
                details = mapOf("status" to "415")
            ).apiUserMessage("fallback")
        )
    }

    @Test
    fun fallsBackToApiMessageThenThrowableMessage() {
        assertEquals(
            "server failed",
            ApiException("/api/item", message = "server failed").apiUserMessage("fallback")
        )
        assertEquals("fallback", IllegalStateException().apiUserMessage("fallback"))
        assertEquals(
            ApiFailureKind.INTERNAL,
            IllegalStateException("broken").apiFailureKind()
        )
        assertEquals(
            ApiFailureKind.UPSTREAM,
            ApiException(
                "/api/item",
                message = "upstream",
                code = F95ApiErrorCode.UPSTREAM_FAILURE
            ).apiFailureKind()
        )
    }
}
