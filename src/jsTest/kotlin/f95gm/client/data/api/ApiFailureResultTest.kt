package f95gm.client.data.api

import f95gm.domain.api.ApiPath
import f95gm.client.model.runtime.HttpStatusCodeValue
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApiFailureResultTest {
    @Test
    fun apiResultExposesTypedFailureAsItsPrimaryError() {
        val result = runSuspend {
            apiResult<String> {
                throw ApiException(ApiPath("/api/login"), status = HttpStatusCodeValue(401), message = "expired")
            }
        }
        assertEquals(ApiFailureKind.AUTHENTICATION, (result as ApiResult.Failure).failure.kind)
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var completion: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { completion = result }
        })
        return assertNotNull(completion).getOrThrow()
    }
}
