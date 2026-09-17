package f95gm.server.upstream.request

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpstreamRequestDefaultsTest {
    @Test
    fun appliesStableProtocolHeadersAndSessionCookies() {
        val builder = HttpRequestBuilder()
        builder.applyUpstreamDefaults(
            HttpMethod.Get,
            UpstreamSession(F95SessionId("session"), F95AccountKey("account"), mapOf("sid" to "value")),
            listOf(UpstreamHeader("X-Test", "yes")),
            null,
            null
        )

        assertEquals(HttpMethod.Get, builder.method)
        assertEquals("sid=value", builder.headers[HttpHeaders.Cookie])
        assertEquals(UpstreamProtocolHeaders.DEFAULT_ACCEPT, builder.headers[HttpHeaders.Accept])
        assertEquals("yes", builder.headers["X-Test"])
    }

    @Test
    fun appliesBodyContentTypeAndAllowsProtocolHeaderOverrides() {
        val builder = HttpRequestBuilder()
        builder.applyUpstreamDefaults(
            HttpMethod.Post,
            UpstreamSession(F95SessionId("session"), F95AccountKey("account"), emptyMap()),
            listOf(
                UpstreamHeader(HttpHeaders.Accept, "application/json"),
                UpstreamHeader("X-Trace", "trace-1")
            ),
            "application/json",
            "{\"ok\":true}"
        )

        assertEquals(HttpMethod.Post, builder.method)
        assertEquals("application/json", builder.headers[HttpHeaders.ContentType])
        assertTrue(builder.headers.getAll(HttpHeaders.Accept).orEmpty().contains("application/json"))
        assertEquals("trace-1", builder.headers["X-Trace"])
    }
}
