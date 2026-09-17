package f95gm.server.app

import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.F95ApiQueryParameters
import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
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
        application { configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle }) }

        val response = client.get("${F95ApiPaths.HEALTH}?${F95ApiQueryParameters.TAB_ID.value}=test-tab")

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
        application { configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle }) }

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
        application { configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle }) }

        val response = client.get(F95ApiPaths.CATALOG)

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(!response.headers[f95gm.server.config.protocol.HttpProtocolConfig.REQUEST_ID_HEADER].isNullOrBlank())
        val error = Json.decodeFromString<F95ApiError>(response.bodyAsText())
        assertEquals(F95ApiMessageKey("server.authenticationRequired"), error.messageKey)
    }

    @Test
    fun everyProtectedReadEndpointRejectsMissingSession() = testApplication {
        val shutdownController = ServerShutdownController {}
        application { configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle }) }

        listOf(
            F95ApiPaths.CATALOG,
            F95ApiPaths.CATALOG_OPTIONS,
            F95ApiPaths.MARKS,
            F95ApiPaths.FILTERS,
            "${F95ApiPaths.ITEM}/123"
        ).forEach { path ->
            assertEquals(HttpStatusCode.Unauthorized, client.get(path).status, path)
        }
    }

    @Test
    fun shutdownRejectsMissingTokenCookie() = testApplication {
        val shutdownController = ServerShutdownController {}
        application { configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle }) }

        assertEquals(HttpStatusCode.Forbidden, client.post(F95ApiPaths.SHUTDOWN).status)
    }

    @Test
    fun mutatingApiRejectsUntrustedOrigin() = testApplication {
        val shutdownController = ServerShutdownController {}
        application {
            configureStatusPages()
            configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle })
        }

        val response = client.post(F95ApiPaths.LOGOUT) {
            header(HttpHeaders.Origin, "https://evil.example")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(f95gm.server.config.protocol.HttpProtocolConfig.REQUEST_ID_HEADER, "client-request-1")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("client-request-1", response.headers[f95gm.server.config.protocol.HttpProtocolConfig.REQUEST_ID_HEADER])
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
                { f95gm.server.desktop.NoopDesktopLifecycle },
                apiRequestsPerMinute = 1,
                rateLimitWindowMillis = 60_000L
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, client.get(F95ApiPaths.CATALOG).status)
        val limited = client.get(F95ApiPaths.CATALOG) {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        assertEquals(HttpStatusCode.TooManyRequests, limited.status)
        assertTrue(!limited.headers[f95gm.server.config.protocol.HttpProtocolConfig.REQUEST_ID_HEADER].isNullOrBlank())
        assertEquals(F95ApiErrorCode.RATE_LIMITED, Json.decodeFromString<F95ApiError>(limited.bodyAsText()).code)
        assertEquals("1", limited.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.LIMIT])
        assertEquals("0", limited.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.REMAINING])
        assertEquals("1", limited.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.IP_LIMIT])
        assertEquals("0", limited.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.IP_REMAINING])
        assertNotNull(limited.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.IP_RESET])
        assertTrue((limited.headers[HttpHeaders.RetryAfter]?.toLongOrNull() ?: 0L) >= 1L)
    }

    @Test
    fun malformedJsonUsesTheSharedApiErrorEnvelopeAndCorrelationHeader() = testApplication {
        val shutdownController = ServerShutdownController {}
        application {
            configureStatusPages()
            configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle })
        }

        val response = client.post(F95ApiPaths.LOGIN) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("{not-json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(!response.headers[f95gm.server.config.protocol.HttpProtocolConfig.REQUEST_ID_HEADER].isNullOrBlank())
        val error = Json.decodeFromString<F95ApiError>(response.bodyAsText())
        assertEquals(F95ApiErrorCode.INVALID_INPUT, error.code)
    }

    @Test
    fun shutdownRejectsWrongAndOverlongTokensWithoutInvokingShutdown() = testApplication {
        var shutdowns = 0
        val shutdownController = ServerShutdownController { shutdowns++ }
        application { configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle }) }

        val wrong = client.post(F95ApiPaths.SHUTDOWN) {
            cookie(f95gm.server.config.protocol.SessionProtocolConfig.SHUTDOWN_COOKIE, "wrong-token")
        }
        val overlong = client.post(F95ApiPaths.SHUTDOWN) {
            cookie(f95gm.server.config.protocol.SessionProtocolConfig.SHUTDOWN_COOKIE, "x".repeat(f95gm.server.config.constants.ServerStorageLimits.COOKIE_VALUE_LENGTH + 1))
        }

        assertEquals(HttpStatusCode.Forbidden, wrong.status)
        assertEquals(HttpStatusCode.Forbidden, overlong.status)
        assertEquals(0, shutdowns)
    }

    @Test
    fun mutatingApiRejectsMalformedOrigin() = testApplication {
        val shutdownController = ServerShutdownController {}
        application {
            configureStatusPages()
            configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle })
        }

        val response = client.post(F95ApiPaths.LOGOUT) {
            header(HttpHeaders.Origin, "not-an-origin")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(F95ApiErrorCode.CSRF_REJECTED, Json.decodeFromString<F95ApiError>(response.bodyAsText()).code)
    }
}
