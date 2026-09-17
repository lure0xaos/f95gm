package f95gm.client.data.api

import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.domain.api.ApiPath
import kotlin.test.Test
import kotlin.test.assertIs

class ApiFailureMapperTest {
    @Test
    fun preservesCsrfFailureType() {
        val error = ApiFailureMapper.fromHttp(
            ApiPath("/api/v1/marks"),
            HttpStatusCodeValue(403),
            """{"message":"CSRF rejected","code":"CSRF_REJECTED"}""",
            "fallback"
        )

        assertIs<ApiCsrfException>(error)
    }

    @Test
    fun preservesBadRequestAndUpstreamFailureTypes() {
        val badRequest = ApiFailureMapper.fromHttp(
            ApiPath("/api/v1/filters"), HttpStatusCodeValue(400),
            """{"message":"Invalid","code":"INVALID_INPUT"}""", "fallback"
        )
        val upstream = ApiFailureMapper.fromHttp(
            ApiPath("/api/v1/catalog"), HttpStatusCodeValue(502),
            """{"message":"Unavailable","code":"UPSTREAM_FAILURE"}""", "fallback"
        )

        assertIs<ApiBadRequestException>(badRequest)
        assertIs<ApiUpstreamException>(upstream)
    }
}
