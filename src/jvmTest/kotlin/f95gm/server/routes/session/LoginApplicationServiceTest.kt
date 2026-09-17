package f95gm.server.routes.session

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.application.SessionRegistry
import f95gm.server.application.SessionService
import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.persistence.sessions.SessionRepository
import f95gm.server.routes.ports.LoginName
import f95gm.server.routes.ports.LoginPassword
import f95gm.server.routes.ports.UpstreamLoginClient
import f95gm.server.routes.ports.UpstreamLoginResult
import f95gm.server.upstream.request.UpstreamRequest
import f95gm.server.upstream.request.UpstreamRequester
import f95gm.server.state.runtime.MutableServerClock
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class LoginApplicationServiceTest {
    private val browserId = F95SessionId("00000000-0000-0000-0000-000000000041")
    private val session = UpstreamSession(browserId, F95AccountKey("account"), mapOf("xf_user" to "user"))
    private val success = UpstreamResponse.Success(HttpStatusCode.OK, "<html>account</html>", ContentType.Text.Html)

    @Test
    fun missingTokenAndUpstreamFailuresReturnTypedRejections() = kotlinx.coroutines.test.runTest {
        val missing = service(UpstreamLoginResult.MissingToken(session)).authenticate(LoginName("user"), LoginPassword("pass"), browserId)
        assertEquals(502, assertIs<LoginApplicationResult.Rejected>(missing).status.value)

        val failure = UpstreamResponse.Failure(
            HttpStatusCode.BadGateway, "", null, reason = UpstreamFailure.TIMEOUT
        )
        val rejected = service(UpstreamLoginResult.Completed(session, success, failure, success))
            .authenticate(LoginName("user"), LoginPassword("pass"), browserId)
        assertEquals(502, assertIs<LoginApplicationResult.Rejected>(rejected).status.value)
    }

    @Test
    fun successfulVerificationPersistsAndRegistersSession() = kotlinx.coroutines.test.runTest {
        var persisted = false
        val repository = object : SessionRepository {
            override suspend fun persist(id: F95SessionId, session: UpstreamSession) { persisted = id == browserId }
            override suspend fun remove(id: F95SessionId) = Unit
            override suspend fun restore(registry: f95gm.server.session.SessionRegistryPort) = Unit
        }
        val clock = MutableServerClock()
        val registry = SessionRegistry(clock)
        val result = service(UpstreamLoginResult.Completed(session, success, success, success), SessionService(repository, registry))
            .authenticate(LoginName("user"), LoginPassword("pass"), browserId)

        assertEquals(true, persisted)
        assertEquals(session, assertIs<LoginApplicationResult.Authenticated>(result).session)
        assertNotNull(registry.lookup(browserId))
    }

    private fun service(
        result: UpstreamLoginResult,
        sessionService: SessionService = SessionService(object : SessionRepository {
            override suspend fun persist(id: F95SessionId, session: UpstreamSession) = Unit
            override suspend fun remove(id: F95SessionId) = Unit
            override suspend fun restore(registry: f95gm.server.session.SessionRegistryPort) = Unit
        }, SessionRegistry())
    ): LoginApplicationService {
        val endpointConfig = UpstreamEndpointConfig.from("https://f95zone.to")
        val upstream = object : UpstreamRequester {
            override val endpointConfig = endpointConfig
            override suspend fun request(request: UpstreamRequest): UpstreamResponse = error("not used")
        }
        val client = object : UpstreamLoginClient {
            override suspend fun authenticate(
                gateway: UpstreamRequester,
                login: LoginName,
                password: LoginPassword,
                browserId: F95SessionId
            ) = result
        }
        val dependencies = object : LoginApplicationDependencies {
            override val sessionService = sessionService
            override val upstreamEndpoints = endpointConfig
            override val upstream = upstream
            override val upstreamLoginClient = client
        }
        return LoginApplicationService(dependencies)
    }
}
