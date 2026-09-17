package f95gm.server.http.response

import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpStatusPredicatesTest {
    @Test
    fun classifiesSuccessRedirectAndClientErrorRanges() {
        assertTrue(HttpStatusCode.OK.isSuccessful)
        assertTrue(HttpStatusCode.MultiStatus.isSuccessful)
        assertFalse(HttpStatusCode.MovedPermanently.isSuccessful)

        assertTrue(HttpStatusCode.Found.isRedirect)
        assertFalse(HttpStatusCode.BadRequest.isRedirect)

        assertTrue(HttpStatusCode.BadRequest.isClientError)
        assertTrue(HttpStatusCode.NotFound.isClientError)
        assertFalse(HttpStatusCode.InternalServerError.isClientError)
    }
}
