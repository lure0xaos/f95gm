package f95gm.client.data.api

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import f95gm.domain.api.ApiPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.promise
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RequestJsonIntegrationTest {
    @Test
    fun requestExecutorEmitsTransportEventsThroughAnInjectedSink() = CoroutineScope(Dispatchers.Default).promise {
        val capturedEvents = mutableListOf<ApiTelemetryEvent>()
        val events = object : ApiClientEvents {
            override fun emit(event: ApiTelemetryEvent) { capturedEvents += event }
        }
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })

        ApiRequestExecutor(ApiTransport(client), events).execute(
            ApiRequestContext(
                path = ApiPath("/api/catalog"),
                method = io.ktor.http.HttpMethod.Get,
                fallback = ApiStatusFallback { "fallback-${it.value}" }
            )
        )

        assertEquals(2, capturedEvents.size)
        assertEquals(ApiPath("/api/catalog"), (capturedEvents[0] as ApiTelemetryEvent.Response).path)
        assertEquals(ApiPath("/api/catalog"), (capturedEvents[1] as ApiTelemetryEvent.Completed).path)
        assertEquals(
            (capturedEvents[0] as ApiTelemetryEvent.Response).requestId,
            (capturedEvents[1] as ApiTelemetryEvent.Completed).requestId
        )
        client.close()
    }

    @Test
    fun requestExecutorEmitsOneCompletionForApiFailures() = CoroutineScope(Dispatchers.Default).promise {
        val capturedEvents = mutableListOf<ApiTelemetryEvent>()
        val events = object : ApiClientEvents {
            override fun emit(event: ApiTelemetryEvent) { capturedEvents += event }
        }
        val client = HttpClient(MockEngine {
            respond("{\"message\":\"bad\",\"code\":\"INVALID_INPUT\"}", HttpStatusCode.BadRequest)
        })

        assertFailsWith<ApiException> {
            ApiRequestExecutor(ApiTransport(client), events).execute(
                ApiRequestContext(
                    path = ApiPath("/api/catalog"),
                    method = io.ktor.http.HttpMethod.Get,
                    fallback = ApiStatusFallback { "fallback-${it.value}" }
                )
            )
        }

        assertEquals(1, capturedEvents.count { it is ApiTelemetryEvent.Completed })
        assertEquals(1, capturedEvents.count { it is ApiTelemetryEvent.Failure })
        assertTrue(capturedEvents[0] is ApiTelemetryEvent.Response)
        assertTrue(capturedEvents[1] is ApiTelemetryEvent.Completed)
        assertTrue(capturedEvents[2] is ApiTelemetryEvent.Failure)
        client.close()
    }

    @Test
    fun requestJsonUsesInjectedTransportAndReturnsTheResponseBody() = CoroutineScope(Dispatchers.Default).promise {
        val client = HttpClient(MockEngine { request ->
            assertEquals("/api/catalog", request.url.encodedPath)
            respond("{\"ok\":true}", HttpStatusCode.OK)
        })

        assertEquals(
            "{\"ok\":true}",
            requestJson(
                path = ApiPath("/api/catalog"),
                method = io.ktor.http.HttpMethod.Get,
                fallback = { "fallback-$it" },
                transport = ApiTransport(client),
                events = NoopApiClientEvents
            )
        )
        client.close()
    }

    @Test
    fun requestJsonConvertsNonSuccessResponsesIntoApiExceptions() = CoroutineScope(Dispatchers.Default).promise {
        val client = HttpClient(MockEngine {
            respond("{\"message\":\"bad\",\"code\":\"INVALID_INPUT\"}", HttpStatusCode.BadRequest)
        })

        val error = assertFailsWith<ApiException> {
            requestJson(
                path = ApiPath("/api/catalog"),
                method = io.ktor.http.HttpMethod.Get,
                fallback = { "fallback-$it" },
                transport = ApiTransport(client),
                events = NoopApiClientEvents
            )
        }
        assertEquals(400, error.status?.value)
        assertEquals("bad", error.message)
        client.close()
    }

    @Test
    fun ioTransportFailuresAreClassifiedAsConnectivityFailures() = CoroutineScope(Dispatchers.Default).promise {
        val client = HttpClient(MockEngine { throw IOException("offline") })

        val error = assertFailsWith<ApiException> {
            requestJson(
                path = ApiPath("/api/catalog"),
                method = io.ktor.http.HttpMethod.Get,
                fallback = { "fallback-$it" },
                transport = ApiTransport(client),
                events = NoopApiClientEvents
            )
        }
        assertEquals(ApiExceptionCause.CONNECTIVITY, error.causeKind)
        client.close()
    }

    @Test
    fun unexpectedClientFailuresPropagateWithoutNormalization() = CoroutineScope(Dispatchers.Default).promise {
        val client = HttpClient(MockEngine { error("invalid request setup") })

        val error = assertFailsWith<IllegalStateException> {
            requestJson(
                path = ApiPath("/api/catalog"),
                method = io.ktor.http.HttpMethod.Get,
                fallback = { "fallback-$it" },
                transport = ApiTransport(client),
                events = NoopApiClientEvents
            )
        }
        assertEquals("invalid request setup", error.message)
        client.close()
    }
}
