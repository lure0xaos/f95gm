package f95gm.server.upstream.request

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class UpstreamClientTest {
    @Test
    fun storesReturnedCookiesAndPersistsTheOwningSession() = runTest {
        val client = HttpClient(MockEngine {
            respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.SetCookie, "xf_session=session-value; Path=/"))
        })
        val session = session()
        var persisted: UpstreamSession? = null
        val response = withContext(Dispatchers.Default) {
            UpstreamClient(
                KtorUpstreamHttpClient(client),
                scope = testScope(),
                persistSession = { persisted = it }
            ).request(HttpMethod.Get, "/", session)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("session-value", session.cookieEntries()["xf_session"])
        assertNotNull(persisted)
        client.close()
    }

    @Test
    fun retriesTransientGetFailures() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine {
            requests++
            if (requests == 1) respond("temporary failure", HttpStatusCode.ServiceUnavailable)
            else respond("ok", HttpStatusCode.OK)
        })
        val response =
            UpstreamClient(KtorUpstreamHttpClient(client), scope = testScope()).request(HttpMethod.Get, "/", session())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(2, requests)
        client.close()
    }

    @Test
    fun getRequestsAreConcurrentForOneSession() = runTest {
        val active = AtomicInteger(0)
        val maximum = AtomicInteger(0)
        val client = HttpClient(MockEngine {
            val current = active.incrementAndGet()
            maximum.updateAndGet { old -> maxOf(old, current) }
            delay(30)
            active.decrementAndGet()
            respond("ok", HttpStatusCode.OK)
        })
        val upstreamClient = UpstreamClient(KtorUpstreamHttpClient(client), scope = testScope())
        withContext(Dispatchers.Default) {
            coroutineScope {
                listOf(
                    async { upstreamClient.request(HttpMethod.Get, "/a", session()) },
                    async { upstreamClient.request(HttpMethod.Get, "/b", session()) }
                ).forEach { it.await() }
            }
        }
        assertTrue(maximum.get() >= 2)
        client.close()
    }

    @Test
    fun mutationRequestsRemainSerializedForOneSession() = runTest {
        val active = AtomicInteger(0)
        val maximum = AtomicInteger(0)
        val client = HttpClient(MockEngine {
            val current = active.incrementAndGet()
            maximum.updateAndGet { old -> maxOf(old, current) }
            delay(30)
            active.decrementAndGet()
            respond("ok", HttpStatusCode.OK)
        })
        val upstreamClient = UpstreamClient(KtorUpstreamHttpClient(client), scope = testScope())
        withContext(Dispatchers.Default) {
            coroutineScope {
                listOf(
                    async { upstreamClient.request(HttpMethod.Post, "/a", session()) },
                    async { upstreamClient.request(HttpMethod.Post, "/b", session()) }
                ).forEach { it.await() }
            }
        }
        assertEquals(1, maximum.get())
        client.close()
    }

    @Test
    fun rejectsUnknownLengthResponsesAboveTheConfiguredLimit() = runTest {
        val client = HttpClient(MockEngine { respond("123456789", HttpStatusCode.OK) })
        val response = UpstreamClient(
            KtorUpstreamHttpClient(client),
            scope = testScope(),
            policy = UpstreamPolicy(maxAttempts = 1, maxResponseBytes = 3)
        ).request(HttpMethod.Get, "/", session())
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        client.close()
    }

    @Test
    fun returnsStructuredFailureAtTheConfiguredTimeout() {
        val client = HttpClient(MockEngine {
            delay(100)
            respond("late", HttpStatusCode.OK)
        })
        var response: f95gm.server.model.http.UpstreamResponse? = null
        runTest {
            response = withContext(Dispatchers.Default) {
                UpstreamClient(
                    KtorUpstreamHttpClient(client),
                    scope = testScope(),
                    policy = UpstreamPolicy(maxAttempts = 1, requestTimeout = 10.milliseconds)
                ).request(HttpMethod.Get, "/", session())
            }
        }
        assertEquals(HttpStatusCode.GatewayTimeout, response?.status)
        assertEquals("TIMEOUT", (response as? f95gm.server.model.http.UpstreamResponse.Failure)?.reason?.name)
        client.close()
    }

    private fun session() = UpstreamSession(F95SessionId("session-id"), F95AccountKey("account"))

    private fun testScope() = CoroutineScope(Dispatchers.Default)
}
