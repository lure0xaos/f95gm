package f95gm.client.data.api

import f95gm.domain.api.F95ApiErrorCode
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ApiExceptionTest {
    @Test
    fun normalizesUnknownFailuresWithoutLosingTheirMessage() {
        val cause = IllegalStateException("database unavailable")

        val normalized = normalizeApiException(cause, path = "/api/catalog", connectivityFailure = true)

        assertEquals("/api/catalog", normalized.path)
        assertEquals("database unavailable", normalized.message)
        assertEquals(true, normalized.connectivityFailure)
        assertSame(cause, normalized.cause)
        assertEquals(ApiFailureKind.CONNECTIVITY, normalized.kind)
    }

    @Test
    fun keepsExistingApiExceptionsAndClassifiesProtocolFailures() {
        val existing =
            ApiException("/api/item", status = 401, message = "expired", messageKey = "server.sessionExpired")
        assertSame(existing, normalizeApiException(existing))
        assertEquals(ApiFailureKind.AUTHENTICATION, existing.kind)

        assertEquals(
            ApiFailureKind.RATE_LIMITED,
            ApiException("/api/catalog", status = 429, message = "slow down", code = F95ApiErrorCode.RATE_LIMITED).kind
        )
        assertEquals(
            ApiFailureKind.VALIDATION,
            ApiException("/api/filter", status = 400, message = "invalid", code = F95ApiErrorCode.INVALID_INPUT).kind
        )
        assertEquals(
            ApiFailureKind.UPSTREAM,
            ApiException("/api/item", status = 502, message = "upstream", code = F95ApiErrorCode.UPSTREAM_FAILURE).kind
        )
        assertEquals(
            ApiFailureKind.AUTHENTICATION,
            ApiException(
                "/api/session",
                status = 403,
                message = "expired session",
                messageKey = "server.sessionExpired"
            ).kind
        )
        assertEquals(
            ApiFailureKind.INTERNAL,
            ApiException("/api/unknown", status = 500, message = "failed").kind
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
            apiResult<String> { error("broken") }
                .onSuccess { successCalls++ }
                .onFailure { failureCalls++ }
        }

        assertEquals(ApiFailureKind.INTERNAL, (failure as ApiResult.Failure).error.kind)
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
        return completion!!.getOrThrow()
    }
}
