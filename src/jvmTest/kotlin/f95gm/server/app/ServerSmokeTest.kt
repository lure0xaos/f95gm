package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.server.desktop.ServerShutdownController
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies the public server boundary in the same order a packaged client encounters it. */
class ServerSmokeTest {
    @Test
    fun servesStaticEntryPointHealthAndProtectedApiBoundary() = testApplication {
        application {
            configureTestRoutes("shutdown-token", { ServerShutdownController {} }, { null })
        }

        val entryPoint = client.get("/")
        assertEquals(HttpStatusCode.OK, entryPoint.status)
        assertTrue(entryPoint.bodyAsText().contains("F95", ignoreCase = true))

        val health = client.get(F95Api.HEALTH)
        assertEquals(HttpStatusCode.OK, health.status)
        assertTrue(health.bodyAsText().isEmpty())

        val metrics = client.get(F95Api.METRICS)
        assertEquals(HttpStatusCode.OK, metrics.status)
        assertTrue(metrics.bodyAsText().contains("requests"))

        val protectedCatalog = client.get(F95Api.CATALOG)
        assertEquals(HttpStatusCode.Unauthorized, protectedCatalog.status)
    }
}
