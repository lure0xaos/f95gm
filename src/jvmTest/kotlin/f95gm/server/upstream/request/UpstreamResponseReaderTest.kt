package f95gm.server.upstream.request

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.config.Bytes
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UpstreamResponseReaderTest {
    @Test
    fun readsBodyMetadataAndPersistsChangedCookies() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = "body",
                status = HttpStatusCode.Created,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Text.Plain.toString()),
                    HttpHeaders.Location to listOf("https://example.test/next"),
                    HttpHeaders.SetCookie to listOf("xf_session=session-value; Path=/")
                )
            )
        })
        val session = session()
        var persisted = 0

        val response = client.get("/").let {
            UpstreamResponseReader(Bytes(100)) { persisted++ }.read(it, session)
        }

        val success = assertIs<UpstreamResponse.Success>(response)
        assertEquals(HttpStatusCode.Created, success.status)
        assertEquals("body", success.body)
        assertEquals(ContentType.Text.Plain, success.contentType)
        assertEquals("https://example.test/next", success.location?.value)
        assertEquals("session-value", session.typedCookieEntries().entries.single { it.key.value == "xf_session" }.value.value)
        assertEquals(1, persisted)
        client.close()
    }

    @Test
    fun rejectsResponsesAboveTheConfiguredByteLimitBeforeReturningTheirBody() = runTest {
        val client = HttpClient(MockEngine {
            respond("too-large", HttpStatusCode.OK)
        })
        val response = client.get("/").let {
            UpstreamResponseReader(Bytes(3)) {}.read(it, session())
        }

        val failure = assertIs<UpstreamResponse.Failure>(response)
        assertEquals(HttpStatusCode.PayloadTooLarge, failure.status)
        assertEquals("", failure.body)
        assertEquals(null, failure.contentType)
        assertEquals(null, failure.location)
        assertEquals(UpstreamFailure.RESPONSE_TOO_LARGE, failure.reason)
        assertNull(failure.retryReason)
        client.close()
    }

    private fun session() = UpstreamSession(
        F95SessionId("00000000-0000-0000-0000-000000000020"),
        F95AccountKey("reader-test")
    )
}
