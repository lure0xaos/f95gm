package f95gm.server.app

import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiMessageKey
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.config.ServerConfig
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.ShutdownToken
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.database.migrateDatabase
import f95gm.server.persistence.testDatabaseUrl
import f95gm.server.persistence.schema.SessionsTable
import f95gm.server.persistence.sessions.persistSession
import f95gm.server.state.runtime.ServerContext
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun database(name: String): Database = Database.connect(
    url = testDatabaseUrl(name),
    driver = "org.h2.Driver"
)

class ConfigureRoutesPersistenceTest {
    @Test
    fun deleteMutationsReplaySuccessfulResponsesForTheSameKey() = testApplication {
        val database = database("idempotent_delete")
        val context =
            ServerContext(ServerConfig.from(emptyMap()), databaseExecutor = DatabaseExecutor(database = { database }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext()))
        migrateDatabase(database)
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000010")
        context.applicationComposition.infrastructure.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        application {
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                RouteConfiguration(),
                context
            )
        }
        try {
            val cookie = "${f95gm.server.config.protocol.SessionProtocolConfig.BROWSER_COOKIE}=${sessionId.value}"
            val firstMark = client.delete("${F95ApiPaths.MARKS}/123") {
                header(HttpHeaders.Cookie, cookie); header(
                "Idempotency-Key",
                "delete-mark-test"
            )
            }
            val replayMark = client.delete("${F95ApiPaths.MARKS}/123") {
                header(HttpHeaders.Cookie, cookie); header(
                "Idempotency-Key",
                "delete-mark-test"
            )
            }
            assertEquals(HttpStatusCode.OK, firstMark.status)
            assertEquals(firstMark.bodyAsText(), replayMark.bodyAsText())
            val firstFilter = client.delete("${F95ApiPaths.FILTERS}/favorites") {
                header(
                    HttpHeaders.Cookie,
                    cookie
                ); header("Idempotency-Key", "delete-filter-test")
            }
            val replayFilter = client.delete("${F95ApiPaths.FILTERS}/favorites") {
                header(
                    HttpHeaders.Cookie,
                    cookie
                ); header("Idempotency-Key", "delete-filter-test")
            }
            assertEquals(HttpStatusCode.OK, firstFilter.status)
            assertEquals(firstFilter.bodyAsText(), replayFilter.bodyAsText())
        } finally {
            context.close()
        }
    }

    @Test
    fun expiredSessionIsClearedFromMemoryAndDatabase() = testApplication {
        val upstream = HttpClient(MockEngine {
            respond(
                "<html data-template=\"login\"><title>Log in</title></html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString())
            )
        })
        val database = database("session_expiration")
        val context = ServerContext(
            ServerConfig.from(emptyMap()),
            httpClient = upstream,
            databaseExecutor = DatabaseExecutor(database = { database }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext())
        )
        migrateDatabase(database)
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000002")
        val session = UpstreamSession(sessionId, F95AccountKey("user"))
        context.applicationComposition.infrastructure.sessions.put(sessionId, session)
        persistSession(DatabaseExecutor(database = { database }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext()), context.applicationComposition.runtime.clock, sessionId, session)
        application {
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                RouteConfiguration(),
                context
            )
        }
        try {
            val response = client.get(F95ApiPaths.SESSION) {
                header(
                    HttpHeaders.Cookie,
                    "${f95gm.server.config.protocol.SessionProtocolConfig.BROWSER_COOKIE}=${sessionId.value}"
                )
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals(
                F95ApiMessageKey("server.sessionExpired"),
                Json.decodeFromString<F95ApiError>(response.bodyAsText()).messageKey
            )
            assertTrue(response.headers[HttpHeaders.SetCookie]?.contains("Max-Age=0") == true)
            assertTrue(context.applicationComposition.infrastructure.sessions.lookup(sessionId) is f95gm.server.session.SessionLookup.Missing)
            assertTrue(transaction(database) { SessionsTable.selectAll().none() })
        } finally {
            context.close()
        }
    }

    @Test
    fun staleSessionCookieIsClearedWhenTheSessionIsMissing() = testApplication {
        val database = database("stale_session_cookie")
        val context =
            ServerContext(ServerConfig.from(emptyMap()), databaseExecutor = DatabaseExecutor(database = { database }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext()))
        migrateDatabase(database)
        application {
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                RouteConfiguration(),
                context
            )
        }
        try {
            val response = client.get(F95ApiPaths.SESSION) {
                header(HttpHeaders.Cookie, "${f95gm.server.config.protocol.SessionProtocolConfig.BROWSER_COOKIE}=missing-session")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertTrue(response.headers[HttpHeaders.SetCookie]?.contains("Max-Age=0") == true)
        } finally {
            context.close()
        }
    }

    @Test
    fun loginAndLogoutUseTheDatabaseBackedSessionBoundary() = testApplication {
        var requestNumber = 0
        val upstream = HttpClient(MockEngine {
            when (++requestNumber) {
                1 -> respond(
                    "<html><input name=\"_xfToken\" value=\"token\"></html>",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString())
                )

                2 -> respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "/sam/latest_alpha/"))
                else -> respond(
                    "<html><title>Home</title></html>",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString())
                )
            }
        })
        val database = database("login_logout")
        val context = ServerContext(
            ServerConfig.from(emptyMap()),
            httpClient = upstream,
            databaseExecutor = DatabaseExecutor(database = { database }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext())
        )
        migrateDatabase(database)
        application {
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                RouteConfiguration(),
                context
            )
        }
        try {
            val loginResponse = client.post(F95ApiPaths.LOGIN) {
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody("login=user&password=secret")
            }
            assertEquals(HttpStatusCode.OK, loginResponse.status)
            val cookie = loginResponse.headers[HttpHeaders.SetCookie]?.substringBefore(';')
                ?: error("Login did not issue a session cookie")
            assertTrue(loginResponse.headers[HttpHeaders.SetCookie]?.contains("HttpOnly") == true)
            assertTrue(loginResponse.headers[HttpHeaders.SetCookie]?.contains("SameSite=Lax") == true)
            assertEquals(HttpStatusCode.OK, client.post(F95ApiPaths.LOGOUT) { header(HttpHeaders.Cookie, cookie) }.status)
            assertTrue(transaction(database) { SessionsTable.selectAll().none() })
        } finally {
            context.close()
        }
    }
}
