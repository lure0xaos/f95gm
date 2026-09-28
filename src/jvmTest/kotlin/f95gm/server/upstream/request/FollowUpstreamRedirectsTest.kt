package f95gm.server.upstream.request

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.config.upstream.UpstreamBaseUrl
import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamLocation
import f95gm.server.model.http.UpstreamResponse
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class FollowUpstreamRedirectsTest {
    private val baseUrl = UpstreamBaseUrl.parse("https://f95zone.to")

    @Test
    fun acceptsRelativeAndSameHostHttpsRedirects() {
        assertEquals(UpstreamPath("/sam/latest_alpha/"), (validateUpstreamRedirect("/sam/latest_alpha/", baseUrl) as UpstreamRedirectResult.Accepted).path)
        assertEquals(UpstreamPath("https://f95zone.to/login/"), (validateUpstreamRedirect("https://f95zone.to/login/", baseUrl) as UpstreamRedirectResult.Accepted).path)
    }

    @Test
    fun rejectsExternalAndProtocolRelativeRedirects() {
        assertIs<UpstreamRedirectResult.Rejected>(validateUpstreamRedirect("https://example.com/steal", baseUrl))
        assertIs<UpstreamRedirectResult.Rejected>(validateUpstreamRedirect("//example.com/steal", baseUrl))
        assertIs<UpstreamRedirectResult.Rejected>(validateUpstreamRedirect("javascript:alert(1)", baseUrl))
    }

    @Test
    fun exposesStructuredRedirectRejectionReasons() {
        assertEquals(
            UpstreamRedirectRejection.PROTOCOL_RELATIVE,
            (validateUpstreamRedirect("//example.com/steal", baseUrl) as UpstreamRedirectResult.Rejected).reason
        )
        assertEquals(
            UpstreamRedirectRejection.EXTERNAL_HOST,
            (validateUpstreamRedirect("https://example.com/steal", baseUrl) as UpstreamRedirectResult.Rejected).reason
        )
        assertEquals(
            UpstreamRedirectRejection.UNSAFE_SCHEME,
            (validateUpstreamRedirect("javascript:alert(1)", baseUrl) as UpstreamRedirectResult.Rejected).reason
        )
        assertEquals(
            UpstreamRedirectRejection.MALFORMED,
            (validateUpstreamRedirect("https://f95zone.to/%zz", baseUrl) as UpstreamRedirectResult.Rejected).reason
        )
    }

    @Test
    fun followsDetailRedirectWithOriginalHeadersAndPolicy() = runTest {
        val requests = mutableListOf<UpstreamRequest>()
        val session = UpstreamSession(F95SessionId("session"), F95AccountKey("account"))
        val gateway = object : UpstreamRequester {
            override val endpointConfig = UpstreamEndpointConfig(baseUrl)
            override suspend fun request(request: UpstreamRequest): UpstreamResponse {
                requests += request
                return UpstreamResponse.Success(HttpStatusCode.OK, "detail", null)
            }
        }
        val headers = UpstreamHeaders.of(UpstreamHeader("X-Test", "retained"))
        val initial = UpstreamResponse.Success(
            HttpStatusCode.Found,
            "",
            null,
            location = UpstreamLocation("/threads/42/redirected")
        )

        val result = followUpstreamRedirects(gateway, initial, session, headers, UpstreamEndpointPolicy.DETAIL)

        assertEquals("detail", result.body)
        assertEquals(1, requests.size)
        assertEquals(UpstreamPath("/threads/42/redirected"), requests.single().path)
        assertEquals(headers.values, requests.single().headers.values)
        assertEquals(UpstreamEndpointPolicy.DETAIL, requests.single().policy)
    }
}
