package f95gm.server.app
import f95gm.domain.api.*

import f95gm.domain.api.F95ApiStatus
import f95gm.domain.api.endpointDescriptors
import f95gm.domain.api.openApiContractJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class F95OpenApiContractTest {
    @Test
    fun contractIsGeneratedFromSharedEndpointDescriptors() {
        val json = openApiContractJson()

        assertContains(json, "\"/api/catalog\"")
        assertContains(json, "\"get\"")
        assertContains(json, "\"session\"")
        assertContains(json, "\"/api/health\"")
    }

    @Test
    fun methodsOnTheSamePathKeepTheirOwnMetadata() {
        val filters = Json.parseToJsonElement(openApiContractJson())
            .jsonObject.requiredObject("paths").requiredObject("/api/filters")

        assertEquals("READ", filters.requiredObject("get").requiredPrimitive("x-f95gm-mutation").content)
        assertEquals("WRITE", filters.requiredObject("post").requiredPrimitive("x-f95gm-mutation").content)
        assertContains(filters.requiredObject("post").keys, "requestBody")
        assertContains(filters.requiredObject("post").requiredObject("responses").keys, "400")
    }

    @Test
    fun operationsDescribeDifferentRequestResponseAndStatusShapes() {
        val contract = Json.parseToJsonElement(openApiContractJson()).jsonObject
        val paths = contract.requiredObject("paths")
        val login = paths.requiredObject("/api/login").requiredObject("post")
        val health = paths.requiredObject("/api/health").requiredObject("get")
        val media = paths.requiredObject("/api/media").requiredObject("get")

        assertEquals(
            "LoginRequest",
            login.requiredObject("requestBody").requiredObject("content").requiredObject("application/json").requiredObject("schema").requiredPrimitive("$" + "ref").content.substringAfterLast(
                '/'
            )
        )
        assertEquals(
            setOf("200", "400", "401", "429", "500", "502", "503", "504"),
            login.requiredObject("responses").keys
        )
        assertEquals(setOf("200"), health.requiredObject("responses").keys)
        assertContains(media.requiredObject("responses").requiredObject("200").requiredObject("content").keys, "image/*")
        assertEquals("url", media.requiredArray("parameters").first().jsonObject.requiredPrimitive("name").content)
    }

    @Test
    fun contractGenerationIsDeterministicAcrossRepeatedCalls() {
        val first = openApiContractJson()
        repeat(5) { assertEquals(first, openApiContractJson()) }
    }

    @Test
    fun endpointMetadataMutationsCannotDisappearFromTheGeneratedContract() {
        val paths = Json.parseToJsonElement(openApiContractJson()).jsonObject.requiredObject("paths")
        endpointDescriptors.forEach { descriptor ->
            descriptor.methods.forEach { method ->
                val operation = paths.requiredObject(descriptor.path.value).requiredObject(method.wireValue.lowercase())
                assertEquals(descriptor.mutation.name, operation.requiredPrimitive("x-f95gm-mutation").content)
                assertEquals(descriptor.rateLimit.name, operation.requiredPrimitive("x-f95gm-rate-limit").content)
                assertEquals(
                    descriptor.auth == AuthRequirement.SESSION,
                    operation.requiredArray("security").isNotEmpty()
                )
                assertEquals(
                    descriptor.errorStatuses.map { it.code.toString() }.toSet() + descriptor.successStatus.code.toString(),
                    operation.requiredObject("responses").keys
                )
                if (descriptor.queryParameters.isNotEmpty() || descriptor.pathParameters.isNotEmpty()) {
                    val parameters = operation.requiredArray("parameters").map {
                        it.jsonObject.requiredPrimitive("name").content
                    }.toSet()
                    assertEquals(
                        (descriptor.queryParameters.map { it.value } + descriptor.pathParameters.map { it.value }).toSet(),
                        parameters
                    )
                }
            }
        }
    }

    @Test
    fun endpointInventoryRejectsInconsistentParameterAndStatusMetadata() {
        val pathMismatch = EndpointDescriptor(
            path = f95gm.domain.api.ApiPath("/api/items/{threadId}"),
            methods = setOf(ApiMethod.GET),
            auth = AuthRequirement.NONE,
            mutation = MutationKind.READ,
            rateLimit = RateLimitScope.NONE,
            tag = f95gm.domain.api.ApiTag.ITEMS,
            documentation = ApiOperationDocumentation("Test operation", "Test operation description."),
            pathParameters = listOf(f95gm.domain.api.ApiPathParameter("otherId"))
        )
        assertFailsWith<IllegalArgumentException> {
            f95gm.domain.api.validateEndpointDescriptors(listOf(pathMismatch))
        }

        val statusOverlap = pathMismatch.copy(
            path = f95gm.domain.api.ApiPath("/api/items"),
            pathParameters = emptyList(),
            errorStatuses = setOf(F95ApiStatus.OK)
        )
        assertFailsWith<IllegalArgumentException> {
            f95gm.domain.api.validateEndpointDescriptors(listOf(statusOverlap))
        }
    }
}
