package f95gm.server.routes.catalog

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.upstream.request.UpstreamEndpointPolicy
import f95gm.server.upstream.request.UpstreamPath
import f95gm.server.upstream.request.UpstreamPaths
import io.ktor.http.HttpHeaders
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogRequestContractTest {
    @Test
    fun catalogAndLiveMarkLookupsShareRequestHeadersAndPolicy() {
        val endpoints = UpstreamEndpointConfig.DEFAULT
        val session = UpstreamSession(F95SessionId("session"), F95AccountKey("account"))
        val request = catalogRequest(UpstreamPath("/catalog"), session, endpoints)

        assertEquals(UpstreamPath("/catalog"), request.path)
        assertEquals(UpstreamEndpointPolicy.CATALOG, request.policy)
        assertEquals(
            listOf(
                HttpHeaders.Referrer to endpoints.latestPage,
                "X-Requested-With" to "XMLHttpRequest",
                HttpHeaders.Accept to "application/json, text/javascript, */*; q=0.01"
            ),
            request.headers.values.map { it.name.value to it.value.value }
        )
    }
}
