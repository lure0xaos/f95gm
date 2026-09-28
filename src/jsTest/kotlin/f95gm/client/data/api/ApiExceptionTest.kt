package f95gm.client.data.api

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.ApiPath
import f95gm.domain.api.F95ApiMessageKey
import f95gm.client.model.runtime.HttpStatusCodeValue
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ApiExceptionTest {
    @Test
    fun normalizesUnknownFailuresWithoutLosingTheirMessage() {
        val cause = IllegalStateException("database unavailable")

        val normalized = normalizeApiException(cause, path = ApiPath("/api/catalog"), causeKind = ApiExceptionCause.CONNECTIVITY)

        assertEquals("/api/catalog", normalized.path.value)
        assertEquals("database unavailable", normalized.message)
        assertEquals(ApiExceptionCause.CONNECTIVITY, normalized.causeKind)
        assertSame(cause, normalized.cause)
        assertEquals(ApiFailureKind.CONNECTIVITY, normalized.kind)
    }

    @Test
    fun keepsExistingApiExceptionsAndClassifiesProtocolFailures() {
        val existing =
            ApiException(ApiPath("/api/item"), status = HttpStatusCodeValue(401), message = "expired", messageKey = F95ApiMessageKey("server.sessionExpired"))
        assertSame(existing, normalizeApiException(existing))
        assertEquals(ApiFailureKind.AUTHENTICATION, existing.kind)

        assertEquals(
            ApiFailureKind.RATE_LIMITED,
            ApiException(ApiPath("/api/catalog"), status = HttpStatusCodeValue(429), message = "slow down", code = F95ApiErrorCode.RATE_LIMITED).kind
        )
        assertEquals(
            ApiFailureKind.VALIDATION,
            ApiException(ApiPath("/api/filter"), status = HttpStatusCodeValue(400), message = "invalid", code = F95ApiErrorCode.INVALID_INPUT).kind
        )
        assertEquals(
            ApiFailureKind.UPSTREAM,
            ApiException(ApiPath("/api/item"), status = HttpStatusCodeValue(502), message = "upstream", code = F95ApiErrorCode.UPSTREAM_FAILURE).kind
        )
        assertEquals(
            ApiFailureKind.AUTHENTICATION,
            ApiException(
                ApiPath("/api/session"),
                status = HttpStatusCodeValue(403),
                message = "expired session",
                messageKey = F95ApiMessageKey("server.sessionExpired")
            ).kind
        )
        assertEquals(
            ApiFailureKind.INTERNAL,
            ApiException(ApiPath("/api/unknown"), status = HttpStatusCodeValue(500), message = "failed").kind
        )
    }

    @Test
    fun apiResultInvokesOnlyTheMatchingContinuation() {
        var successCalls = 0
        var failureCalls = 0
        val success = runSuspend {
            apiResult { "ok" }
                .onSuccess { successCalls++ }
                .onFailure { failureCalls++ }
        }

        assertEquals(ApiResult.Success("ok"), success)
        assertEquals(1, successCalls)
        assertEquals(0, failureCalls)

        val failure = runSuspend {
            apiResult<String> {
                throw ApiException(ApiPath("/api/catalog"), status = HttpStatusCodeValue(500), message = "broken")
            }
                .onSuccess { successCalls++ }
                .onFailure { failureCalls++ }
        }

        assertEquals(ApiFailureKind.INTERNAL, (failure as ApiResult.Failure).failure.exception.kind)
        assertEquals(1, successCalls)
        assertEquals(1, failureCalls)
    }

    @Test
    fun apiResultRethrowsCancellation() {
        try {
            runSuspend { apiResult<Unit> { throw CancellationException("cancelled") } }
            error("Expected cancellation")
        } catch (cancelled: CancellationException) {
            assertEquals("cancelled", cancelled.message)
        }
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var completion: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) {
                completion = result
            }
        })
        return assertNotNull(completion).getOrThrow()
    }
}
