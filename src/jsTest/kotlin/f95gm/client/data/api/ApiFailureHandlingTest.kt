package f95gm.client.data.api

import f95gm.domain.api.ApiPath
import f95gm.client.model.runtime.HttpStatusCodeValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiFailureHandlingTest {
    @Test
    fun typedFailuresPreserveTheirClassificationWithoutExceptionInspection() {
        val failure = ApiException(
            path = ApiPath("/api/session"),
            status = HttpStatusCodeValue(401),
            message = "expired"
        ).failure

        assertEquals(ApiFailureKind.AUTHENTICATION, failure.apiFailureKind())
    }
}
