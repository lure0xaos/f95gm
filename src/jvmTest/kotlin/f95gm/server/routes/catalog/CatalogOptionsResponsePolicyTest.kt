package f95gm.server.routes.catalog

import f95gm.server.kernel.ServerInvariantViolation
import f95gm.server.model.ParseResult
import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.http.UpstreamResponse
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogOptionsResponsePolicyTest {
    @Test
    fun mapsUnauthenticatedAndInvalidResultsToClientFacingFailures() {
        assertEquals(401, CatalogOptionsResult.Unauthenticated.failureResponse().status.value)
        assertEquals(502, CatalogOptionsResult.Invalid(InvalidReason.MISSING_ASSIGNMENT).failureResponse().status.value)
        assertEquals(502, CatalogOptionsResult.Invalid(InvalidReason.INVALID_JSON).failureResponse().status.value)
    }

    @Test
    fun upstreamAndSuccessfulResultsMustUseTheirOwnResponsePolicies() {
        val upstream = UpstreamResponse.Failure(HttpStatusCode.BadGateway, "", null, reason = UpstreamFailure.TIMEOUT)
        assertFailsWith<ServerInvariantViolation> { CatalogOptionsResult.UpstreamFailure(upstream).failureResponse() }
        assertFailsWith<ServerInvariantViolation> {
            CatalogOptionsResult.Success(
                f95gm.server.model.catalog.CatalogOptionsResponse(
                    kotlinx.serialization.json.JsonObject(emptyMap()), kotlinx.serialization.json.JsonArray(emptyList())
                )
            ).failureResponse()
        }
        assertEquals(502, catalogUpstreamFallbackResponse(upstream).status.value)
    }
}
