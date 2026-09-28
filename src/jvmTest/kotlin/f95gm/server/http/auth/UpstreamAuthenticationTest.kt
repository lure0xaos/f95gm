package f95gm.server.http.auth

import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.model.http.userFacingBody
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpstreamAuthenticationTest {
    @Test
    fun recognizesSuccessfulAuthenticatedResponses() {
        assertTrue(
            isAuthenticatedUpstreamResponse(
                UpstreamResponse.Success(
                    HttpStatusCode.OK,
                    "<html><body>Welcome</body></html>",
                    null
                ), "https://f95zone.to"
            )
        )
        assertTrue(
            isAuthenticatedUpstreamResponse(
                UpstreamResponse.Success(
                    HttpStatusCode.PartialContent,
                    "content",
                    null
                ), "https://f95zone.to"
            )
        )
    }

    @Test
    fun rejectsNonSuccessAndRecognizedLoginBodies() {
        assertFalse(
            isAuthenticatedUpstreamResponse(
                UpstreamResponse.Success(
                    HttpStatusCode.Unauthorized,
                    "Welcome",
                    null
                ), "https://f95zone.to"
            )
        )
        assertFalse(
            isAuthenticatedUpstreamResponse(
                UpstreamResponse.Success(
                    HttpStatusCode.OK,
                    "<html data-template=\"login\"><title>Log in</title></html>",
                    null
                ), "https://f95zone.to"
            )
        )
        assertFalse(
            isAuthenticatedUpstreamResponse(
                UpstreamResponse.Success(
                    HttpStatusCode.OK,
                    "<p>You have to be logged in to access this page</p>",
                    null
                ), "https://f95zone.to"
            )
        )
        assertTrue(isUnauthenticatedUpstreamBody("<title>Log in to F95zone</title>", "https://f95zone.to"))
    }

    @Test
    fun failuresExposeSafeUserFacingTextInsteadOfAnEmptyBody() {
        val response = UpstreamResponse.Failure(HttpStatusCode.BadGateway, "", null, reason = UpstreamFailure.TIMEOUT)

        assertEquals(UpstreamFailure.TIMEOUT.userMessage, response.userFacingBody)
    }
}
