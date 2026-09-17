package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpErrorTest {
    @Test
    fun specializedExceptionsExposeConsistentProtocolMetadata() {
        val badRequest =
            ApiBadRequestException(
                "bad", F95ApiMessageKey("server.invalidRequest"), ApiFailureDetails.field("name")
            )
        assertEquals(HttpStatusCode.BadRequest, badRequest.status)
        assertEquals(F95ApiErrorCode.INVALID_INPUT, badRequest.code)
        assertEquals("server.invalidRequest", badRequest.messageKey?.value)
        assertEquals(mapOf("field" to "name"), badRequest.details.asMap())

        assertEquals(F95ApiErrorCode.CSRF_REJECTED, ApiCsrfException().code)
        assertEquals(HttpStatusCode.Forbidden, ApiCsrfException().status)
        assertEquals(F95ApiErrorCode.UPSTREAM_FAILURE, ApiUpstreamException().code)
        assertEquals(HttpStatusCode.BadGateway, ApiUpstreamException().status)
        assertEquals(F95ApiErrorCode.REQUEST_TOO_LARGE, ApiRequestTooLargeException().code)
        assertEquals(HttpStatusCode.PayloadTooLarge, ApiRequestTooLargeException().status)
        assertEquals(F95ApiErrorCode.RATE_LIMITED, ApiRateLimitedException().code)
        assertEquals(HttpStatusCode.TooManyRequests, ApiRateLimitedException().status)
    }
}
