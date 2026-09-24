package f95gm.server.http.request

import f95gm.server.app.configureTestRoutes
import f95gm.server.config.constants.ServerConstants
import f95gm.server.desktop.ServerShutdownController
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.MDC
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestContextTest {
    @Test
    fun requestContextRemainsBoundToTheIndividualCall() = testApplication {
        application {
            configureTestRoutes("shutdown-token", { ServerShutdownController {} }, { null })
            routing {
                get("/context-probe") {
                    val requestId = call.requestContext.requestId.value
                    delay(20)
                    call.respondText(requestId)
                }
                get("/context-mdc") {
                    call.respondText(MDC.get("requestId") ?: "missing")
                }
            }
        }

        coroutineScope {
            val responses = (1..8).map { index ->
                async {
                    client.get("/context-probe") {
                        header(ServerConstants.REQUEST_ID_HEADER, "test-request-$index")
                    }.bodyAsText()
                }
            }.awaitAll()
            assertEquals((1..8).map { "test-request-$it" }.toSet(), responses.toSet())
        }
    }

    @Test
    fun requestIdIsPropagatedToTheLoggingMdc() = testApplication {
        application {
            configureTestRoutes("shutdown-token", { ServerShutdownController {} }, { null })
            routing {
                get("/context-mdc") {
                    call.respondText(MDC.get("requestId") ?: "missing")
                }
            }
        }

        assertEquals("mdc-request", client.get("/context-mdc") {
            header(ServerConstants.REQUEST_ID_HEADER, "mdc-request")
        }.bodyAsText())
    }
}
