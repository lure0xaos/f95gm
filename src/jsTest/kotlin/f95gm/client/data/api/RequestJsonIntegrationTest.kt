package f95gm.client.data.api

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
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
    fun requestJsonUsesInjectedTransportAndReturnsTheResponseBody() = CoroutineScope(Dispatchers.Default).promise {
        val client = HttpClient(MockEngine { request ->
            assertEquals("/api/catalog", request.url.encodedPath)
            respond("{\"ok\":true}", HttpStatusCode.OK)
        })

        assertEquals(
            "{\"ok\":true}",
            requestJson(
                path = "/api/catalog",
                method = io.ktor.http.HttpMethod.Get,
                fallback = { "fallback-$it" },
                transport = ApiTransport(client)
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
                path = "/api/catalog",
                method = io.ktor.http.HttpMethod.Get,
                fallback = { "fallback-$it" },
                transport = ApiTransport(client)
            )
        }
        assertEquals(400, error.status)
        assertEquals("bad", error.message)
        client.close()
    }

    @Test
    fun ioTransportFailuresAreClassifiedAsConnectivityFailures() = CoroutineScope(Dispatchers.Default).promise {
        val client = HttpClient(MockEngine { throw IOException("offline") })

        val error = assertFailsWith<ApiException> {
            requestJson(
                path = "/api/catalog",
                method = io.ktor.http.HttpMethod.Get,
                fallback = { "fallback-$it" },
                transport = ApiTransport(client)
            )
        }
        assertTrue(error.connectivityFailure)
        client.close()
    }

    @Test
    fun unexpectedClientFailuresAreNotClassifiedAsConnectivityFailures() = CoroutineScope(Dispatchers.Default).promise {
        val client = HttpClient(MockEngine { error("invalid request setup") })

        val error = assertFailsWith<ApiException> {
            requestJson(
                path = "/api/catalog",
                method = io.ktor.http.HttpMethod.Get,
                fallback = { "fallback-$it" },
                transport = ApiTransport(client)
            )
        }
        assertEquals(false, error.connectivityFailure)
        client.close()
    }
}
