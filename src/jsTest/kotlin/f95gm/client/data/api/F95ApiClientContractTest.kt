package f95gm.client.data.api

import f95gm.domain.api.ApiResponseEnvelope
import f95gm.domain.api.ApiStatusCode
import f95gm.domain.api.endpointDescriptors
import f95gm.domain.api.operationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class F95ApiClientContractTest {
    @Test
    fun clientOperationInventoryMatchesTheSharedContract() {
        val expected = endpointDescriptors.map { descriptor ->
            descriptor.methods.map { method ->
                descriptor.operationId(method)
            }
        }.flatten().toSet()

        assertEquals(expected, GeneratedF95ApiOperations.operationIds)
    }

    @Test
    fun responseEnvelopeRetainsStatusAndBody() {
        val response = ApiResponseEnvelope(ApiStatusCode(200), "payload")
        assertEquals(200, response.status)
        assertEquals("payload", response.body)
    }

    @Test
    fun generatedClientOperationsRoundTripSharedEndpointDescriptors() {
        endpointDescriptors.forEach { descriptor ->
            descriptor.methods.forEach { method ->
                val operationId = descriptor.operationId(method)
                val generated = GeneratedF95ApiOperations.byId[operationId]
                assertNotNull(generated)
                assertEquals(descriptor.path.value, generated.path.value)
                assertEquals(method, generated.method)
                assertEquals(descriptor.requestSchema, generated.requestSchema)
                assertEquals(descriptor.responseSchema, generated.responseSchema)
            }
        }
    }

    @Test
    fun generatedOperationsHaveStableIdsAndContractMetadata() {
        assertEquals(
            GeneratedF95ApiOperations.operations.size,
            GeneratedF95ApiOperations.operations.map { it.id }.toSet().size
        )
        GeneratedF95ApiOperations.operations.forEach { operation ->
            assertEquals(operation, GeneratedF95ApiOperations.byId[operation.id])
            assertEquals(operation.id, endpointDescriptors
                .flatMap { descriptor -> descriptor.methods.map { descriptor.operationId(it) } }
                .first { it == operation.id })
        }
    }
}
