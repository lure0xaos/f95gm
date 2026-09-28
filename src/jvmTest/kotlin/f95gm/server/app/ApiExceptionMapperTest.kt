package f95gm.server.app

import f95gm.domain.api.F95ApiErrorCode
import f95gm.server.http.response.ApiBadRequestException
import f95gm.server.http.response.ApiCsrfException
import io.ktor.http.*
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiExceptionMapperTest {
    @Test
    fun mapsKnownExceptionsToTypedApiErrors() {
        assertEquals(HttpStatusCode.BadRequest, mapApiException(ApiBadRequestException("bad"))?.status)
        assertEquals(HttpStatusCode.BadGateway, mapApiException(IOException("upstream"))?.status)
        assertNull(mapApiException(IllegalStateException("unknown")))
    }

    @Test
    fun preservesApplicationExceptionMetadata() {
        val mapped = mapApiException(
            ApiCsrfException("denied")
        )
        assertEquals(HttpStatusCode.Forbidden, mapped?.status)
        assertEquals(F95ApiErrorCode.CSRF_REJECTED, mapped?.error?.code)
    }
}
