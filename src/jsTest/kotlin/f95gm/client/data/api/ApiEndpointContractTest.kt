package f95gm.client.data.api
import f95gm.domain.api.*

import f95gm.domain.api.F95ApiQueryParameters
import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.endpointContract
import f95gm.domain.api.endpointDescriptors
import f95gm.domain.api.operation
import f95gm.domain.api.validateEndpointDescriptors
import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import f95gm.domain.filters.F95Category
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApiEndpointContractTest {
    @Test
    fun everyEndpointUsesTheVersionedApiPrefix() {
        val endpoints = endpointContract
        assertTrue(endpoints.all { it.startsWith("${F95ApiPaths.BASE}/") })
        assertEquals(endpoints.size, endpoints.toSet().size)
    }

    @Test
    fun endpointDescriptorsDeclareRoutingPolicy() {
        assertEquals(endpointContract, endpointDescriptors.map { it.path.value }.distinct())
        assertTrue(endpointDescriptors.all { it.path.value.startsWith(F95ApiPaths.BASE + "/") })
        assertTrue(endpointDescriptors.all { it.methods.size == 1 })
        assertTrue(endpointDescriptors.any {
            it.path.value == F95ApiPaths.SHUTDOWN &&
                    it.methods == setOf(ApiMethod.POST) &&
                    it.rateLimit == RateLimitScope.IP
        })
        assertTrue(endpointDescriptors.any {
            it.path.value == F95ApiPaths.LOGIN &&
                    it.methods == setOf(ApiMethod.POST) &&
                    it.auth == AuthRequirement.NONE
        })
        assertTrue(endpointDescriptors.any {
            it.mutation == MutationKind.WRITE && it.auth == AuthRequirement.SESSION
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
        assertEquals("${F95ApiPaths.MARKS}/123", F95ApiPaths.markPath(F95ThreadId("123")).value)
        assertEquals("${F95ApiPaths.FILTERS}/favorites", F95ApiPaths.filterPath(F95FilterId("favorites")).value)
        assertEquals("${F95ApiPaths.ITEM}/123", F95ApiPaths.itemPath(F95ThreadId("123")).value)
        assertEquals("${F95ApiPaths.CATALOG_OPTIONS}?${F95ApiQueryParameters.CATEGORY.value}=games", F95ApiPaths.catalogOptionsPath(F95Category.GAMES).value)
        assertEquals(
            "${F95ApiPaths.HEALTH}?probe=1&tabId=tab-id&theme=dark",
            F95ApiPaths.healthPath(ApiProbeNumber(1), ApiBrowserTabId("tab-id"), ApiTheme("dark")).value
        )
    }

    @Test
    fun contractOperationsRemainUniqueAcrossEveryDeclaredEndpoint() {
        val operations = endpointDescriptors.flatMap { descriptor ->
            descriptor.methods.map { method ->
                operation(descriptor.path.value, method).operationId
            }
        }
        assertEquals(operations.size, operations.toSet().size)
        assertTrue(operations.all { it.value.matches(Regex("[a-z][a-zA-Z0-9_]*")) })
    }

    @Test
    fun contractValidatorRejectsMutatedDuplicateEndpointInventory() {
        val original = endpointDescriptors
        val duplicate = original + original.first()
        assertFailsWith<IllegalArgumentException> { validateEndpointDescriptors(duplicate) }
    }
}
