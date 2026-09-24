package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.server.config.constants.ServerConstants
import f95gm.server.desktop.ServerShutdownController
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigureRoutesSecurityTest {
    @Test
    fun healthEndpointReturnsNoStoreAndAcceptsHeartbeat() = testApplication {
        val shutdownController = ServerShutdownController {}
        application { configureTestRoutes("shutdown-token", { shutdownController }, { null }) }

        val response = client.get("${F95Api.HEALTH}?${F95Api.TAB_ID}=test-tab")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers["Cache-Control"])
        assertEquals("nosniff", response.headers["X-Content-Type-Options"])
        assertEquals("strict-origin-when-cross-origin", response.headers["Referrer-Policy"])
        assertEquals("DENY", response.headers["X-Frame-Options"])
        assertEquals("camera=(), geolocation=(), microphone=()", response.headers["Permissions-Policy"])
        assertTrue(response.headers["Content-Security-Policy"]?.contains("default-src 'self'") == true)
        assertTrue(!response.headers["X-Request-ID"].isNullOrBlank())
        assertTrue(response.bodyAsText().isEmpty())
    }

    @Test
    fun browserShellStyleNonceMatchesThePerResponseContentSecurityPolicy() = testApplication {
        val shutdownController = ServerShutdownController {}
        application { configureTestRoutes("shutdown-token", { shutdownController }, { null }) }

        val response = client.get("/")
        val policy = response.headers["Content-Security-Policy"].orEmpty()
        val nonce = Regex("style-src 'self' 'nonce-([^']+)'").find(policy)?.groupValues?.get(1)

        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(nonce)
        assertTrue(response.bodyAsText().contains("id=\"fritz2-global-styles\" nonce=\"$nonce\""))
        assertEquals("no-store", response.headers["Cache-Control"])
        assertEquals(null, response.headers[HttpHeaders.ETag])
    }

    @Test
    fun protectedCatalogRejectsMissingSession() = testApplication {
        val shutdownController = ServerShutdownController {}
        application { configureTestRoutes("shutdown-token", { shutdownController }, { null }) }

        val response = client.get(F95Api.CATALOG)

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(!response.headers[ServerConstants.REQUEST_ID_HEADER].isNullOrBlank())
        val error = Json.decodeFromString<F95ApiError>(response.bodyAsText())
        assertEquals(F95ApiMessageKey("server.authenticationRequired"), error.messageKey)
    }

    @Test
    fun everyProtectedReadEndpointRejectsMissingSession() = testApplication {
        val shutdownController = ServerShutdownController {}
        application { configureTestRoutes("shutdown-token", { shutdownController }, { null }) }

        listOf(
            F95Api.CATALOG,
            F95Api.CATALOG_OPTIONS,
            F95Api.MARKS,
            F95Api.FILTERS,
            "${F95Api.ITEM}/123"
        ).forEach { path ->
            assertEquals(HttpStatusCode.Unauthorized, client.get(path).status, path)
        }
    }

    @Test
    fun shutdownRejectsMissingTokenCookie() = testApplication {
        val shutdownController = ServerShutdownController {}
        application { configureTestRoutes("shutdown-token", { shutdownController }, { null }) }

        assertEquals(HttpStatusCode.Forbidden, client.post(F95Api.SHUTDOWN).status)
    }

    @Test
    fun mutatingApiRejectsUntrustedOrigin() = testApplication {
        val shutdownController = ServerShutdownController {}
        application {
            configureStatusPages()
            configureTestRoutes("shutdown-token", { shutdownController }, { null })
        }

        val response = client.post(F95Api.LOGOUT) {
            header(HttpHeaders.Origin, "https://evil.example")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(ServerConstants.REQUEST_ID_HEADER, "client-request-1")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("client-request-1", response.headers[ServerConstants.REQUEST_ID_HEADER])
        assertEquals(F95ApiErrorCode.CSRF_REJECTED, Json.decodeFromString<F95ApiError>(response.bodyAsText()).code)
    }

    @Test
    fun apiRateLimitRejectsRequestsAfterConfiguredBudget() = testApplication {
        val shutdownController = ServerShutdownController {}
        application {
            configureStatusPages()
            configureTestRoutes(
                "shutdown-token",
                { shutdownController },
                { null },
                apiRequestsPerMinute = 1,
                rateLimitWindowMillis = 60_000L
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, client.get(F95Api.CATALOG).status)
        val limited = client.get(F95Api.CATALOG) {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        assertEquals(HttpStatusCode.TooManyRequests, limited.status)
        assertTrue(!limited.headers[ServerConstants.REQUEST_ID_HEADER].isNullOrBlank())
        assertEquals(F95ApiErrorCode.RATE_LIMITED, Json.decodeFromString<F95ApiError>(limited.bodyAsText()).code)
        assertEquals("1", limited.headers[ServerConstants.RATE_LIMIT_HEADER])
        assertEquals("0", limited.headers[ServerConstants.RATE_LIMIT_REMAINING_HEADER])
        assertEquals("1", limited.headers[ServerConstants.RATE_LIMIT_IP_LIMIT_HEADER])
        assertEquals("0", limited.headers[ServerConstants.RATE_LIMIT_IP_REMAINING_HEADER])
        assertNotNull(limited.headers[ServerConstants.RATE_LIMIT_IP_RESET_HEADER])
        assertTrue((limited.headers[HttpHeaders.RetryAfter]?.toLongOrNull() ?: 0L) >= 1L)
    }

    @Test
    fun malformedJsonUsesTheSharedApiErrorEnvelopeAndCorrelationHeader() = testApplication {
        val shutdownController = ServerShutdownController {}
        application {
            configureStatusPages()
            configureTestRoutes("shutdown-token", { shutdownController }, { null })
        }

        val response = client.post(F95Api.LOGIN) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("{not-json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(!response.headers[ServerConstants.REQUEST_ID_HEADER].isNullOrBlank())
        val error = Json.decodeFromString<F95ApiError>(response.bodyAsText())
        assertEquals(F95ApiErrorCode.INVALID_INPUT, error.code)
    }
}
