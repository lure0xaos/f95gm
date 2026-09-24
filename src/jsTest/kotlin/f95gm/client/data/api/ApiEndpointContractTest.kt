package f95gm.client.data.api

import f95gm.domain.api.F95Api
import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiEndpointContractTest {
    @Test
    fun everyEndpointUsesTheVersionedApiPrefix() {
        val endpoints = F95Api.endpointContract
        assertTrue(endpoints.all { it.startsWith("${F95Api.BASE}/") })
        assertEquals(endpoints.size, endpoints.toSet().size)
    }

    @Test
    fun endpointDescriptorsDeclareRoutingPolicy() {
        assertEquals(F95Api.endpointContract, F95Api.endpointDescriptors.map { it.path }.distinct())
        assertTrue(F95Api.endpointDescriptors.all { it.path.startsWith("${F95Api.BASE}/") })
        assertTrue(F95Api.endpointDescriptors.all { it.methods.size == 1 })
        assertTrue(F95Api.endpointDescriptors.any {
            it.path == F95Api.SHUTDOWN &&
                    it.methods == setOf(F95Api.ApiMethod.POST) &&
                    it.rateLimit == F95Api.RateLimitScope.IP
        })
        assertTrue(F95Api.endpointDescriptors.any {
            it.path == F95Api.LOGIN &&
                    it.methods == setOf(F95Api.ApiMethod.POST) &&
                    it.auth == F95Api.AuthRequirement.NONE
        })
        assertTrue(F95Api.endpointDescriptors.any {
            it.mutation == F95Api.MutationKind.WRITE && it.auth == F95Api.AuthRequirement.SESSION
        })
    }

    @Test
    fun errorEnvelopePreservesMachineReadableCode() {
        val decoded = Json.decodeFromString<F95ApiError>("{\"message\":\"bad\",\"code\":\"INVALID_INPUT\"}")
        assertEquals("bad", decoded.message)
        assertEquals(F95ApiErrorCode.INVALID_INPUT, decoded.code)
    }

    @Test
    fun dynamicPathsUseTheSameApiContractAsStaticEndpoints() {
        assertEquals("${F95Api.MARKS}/123", F95Api.markPath(F95ThreadId("123")))
        assertEquals("${F95Api.FILTERS}/favorites", F95Api.filterPath(F95FilterId("favorites")))
        assertEquals("${F95Api.ITEM}/123", F95Api.itemPath(F95ThreadId("123")))
        assertEquals("${F95Api.CATALOG_OPTIONS}?${F95Api.CATEGORY}=games", F95Api.catalogOptionsPath("games"))
    }
}
