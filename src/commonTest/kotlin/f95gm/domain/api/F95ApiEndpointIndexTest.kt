package f95gm.domain.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertFailsWith

class F95ApiEndpointIndexTest {
    @Test
    fun parametersRetainLocationAndTypeAndAreMaterializedOnce() {
        val page = parameterDescriptor(F95ApiQueryParameters.PAGE)
        assertEquals(ParameterLocation.QUERY, page.location)
        assertEquals(ParameterType.INTEGER, page.type)
        assertSame(page, parameterDescriptor(F95ApiQueryParameters.PAGE))
        assertEquals(ParameterType.STRING_ARRAY, parameterDescriptor(F95ApiQueryParameters.TAGS).type)
        assertEquals(ParameterLocation.PATH, parameterDescriptor(ApiPathParameter(F95ApiPaths.THREAD_ID)).location)
        assertFailsWith<ApiContractInvariantViolation> { parameterDescriptor(ApiPathParameter("page")) }
        assertFailsWith<ApiContractInvariantViolation> { parameterDescriptor(ApiQueryParameter("missing")) }
    }

    @Test
    fun validatorRejectsDifferentPathsWhoseOperationIdsCollide() {
        val template = endpointDescriptors.first().copy(methods = setOf(ApiMethod.GET), pathParameters = emptyList())
        val nested = template.copy(path = ApiPath("${F95ApiPaths.BASE}/a/b"))
        val flattened = template.copy(path = ApiPath("${F95ApiPaths.BASE}/a_b"))
        assertFailsWith<IllegalArgumentException> { validateEndpointDescriptors(listOf(nested, flattened)) }
    }

    @Test
    fun endpointIndexContainsExactlyOneDescriptorForEveryMethodPathPair() {
        val descriptors = endpointDescriptors
        assertSame(descriptors, endpointDescriptors)
        val expectedKeys = descriptors.flatMap { descriptor ->
            descriptor.methods.map { method -> ApiOperationKey(descriptor.path, method) }
        }

        assertEquals(expectedKeys.size, expectedKeys.toSet().size)
        assertEquals(expectedKeys.size, endpointOperationIndex.size)
        expectedKeys.forEach { key ->
            assertNotNull(endpointOperationIndex[key])
        }
    }

    @Test
    fun indexedOperationsPreserveDescriptorMetadata() {
        endpointDescriptors.forEach { descriptor ->
            descriptor.methods.forEach { method ->
                val operation = operation(descriptor.path, method)
                assertEquals(descriptor.path.value, operation.path.value)
                assertEquals(method, operation.method)
                assertEquals(descriptor.operationId(method), operation.operationId)
            }
        }
    }
}
