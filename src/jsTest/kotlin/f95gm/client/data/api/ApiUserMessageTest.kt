package f95gm.client.data.api

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.ApiErrorDetailsContract
import f95gm.domain.api.ApiPath
import f95gm.domain.api.F95ApiMessageKey
import f95gm.client.model.runtime.HttpStatusCodeValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiUserMessageTest {
    @Test
    fun mapsKnownServerMessageKeysToLocalizedUserText() {
        assertEquals(
            "Sign in is required.",
            ApiException(ApiPath("/api/catalog"), message = "ignored", messageKey = F95ApiMessageKey("server.authenticationRequired"))
                .apiUserMessage("fallback")
        )
        assertEquals(
            "F95zone login failed (401).",
            ApiException(ApiPath("/api/login"), status = HttpStatusCodeValue(401), message = "ignored", messageKey = F95ApiMessageKey("server.loginFailed"))
                .apiUserMessage("fallback")
        )
        assertEquals(
            "F95zone media returned 415.",
            ApiException(
                ApiPath("/api/media"),
                status = HttpStatusCodeValue(502),
                message = "ignored",
                messageKey = F95ApiMessageKey("server.mediaStatus"),
                details = ApiErrorDetailsContract(status = 415)
            ).apiUserMessage("fallback")
        )
    }

    @Test
    fun fallsBackToApiMessageThenThrowableMessage() {
        assertEquals(
            "server failed",
            ApiException(ApiPath("/api/item"), message = "server failed").apiUserMessage("fallback")
        )
        assertEquals("fallback", IllegalStateException().apiUserMessage("fallback"))
        assertEquals(
            ApiFailureKind.INTERNAL,
            IllegalStateException("broken").apiFailureKind()
        )
        assertEquals(
            ApiFailureKind.UPSTREAM,
            ApiException(
                ApiPath("/api/item"),
                message = "upstream",
                code = F95ApiErrorCode.UPSTREAM_FAILURE
            ).apiFailureKind()
        )
    }
}

