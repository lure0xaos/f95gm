package f95gm.server.routes.session

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthorizationContextTest {
    @Test
    fun keepsPrincipalAndSessionIdentityAligned() {
        val id = F95SessionId("00000000-0000-0000-0000-000000000001")
        val session = UpstreamSession(id, F95AccountKey("account"))
        val context = AuthorizationContext(AuthenticatedPrincipal(id, session.accountKey), session)

        assertEquals(context.principal.sessionId, context.session.browserId)
        assertEquals(context.principal.accountKey, context.session.accountKey)
    }

    @Test
    fun attachesAndReadsAuthorizationContextFromAnApplicationCall() = testApplication {
        application {
            routing {
                get("/context") {
                    val id = F95SessionId("00000000-0000-0000-0000-000000000002")
                    val session = UpstreamSession(id, F95AccountKey("account"))
                    call.attachAuthenticatedSession(session)
                    val context = requireNotNull(call.authorizationContextOrNull())
                    call.respondText("${context.principal.accountKey.value}:${context.session.browserId.value}")
                }
            }
        }
        assertEquals(
            "account:00000000-0000-0000-0000-000000000002",
            client.get("/context").bodyAsText()
        )
    }

    @Test
    fun returnsNoContextUntilBothPrincipalAndSessionAreAttached() = testApplication {
        application {
            routing {
                get("/context") {
                    assertNull(call.authorizationContextOrNull())
                    call.respondText("empty")
                }
            }
        }
        assertEquals("empty", client.get("/context").bodyAsText())
    }
}
