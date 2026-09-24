package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.domain.api.openApiContractJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class F95OpenApiContractTest {
    @Test
    fun contractIsGeneratedFromSharedEndpointDescriptors() {
        val json = F95Api.openApiContractJson()

        assertContains(json, "\"/api/catalog\"")
        assertContains(json, "\"get\"")
        assertContains(json, "\"session\"")
        assertContains(json, "\"/api/health\"")
    }

    @Test
    fun methodsOnTheSamePathKeepTheirOwnMetadata() {
        val filters = Json.parseToJsonElement(F95Api.openApiContractJson())
            .jsonObject["paths"]!!.jsonObject["/api/filters"]!!.jsonObject

        assertEquals("READ", filters["get"]!!.jsonObject["x-f95gm-mutation"]!!.jsonPrimitive.content)
        assertEquals("WRITE", filters["post"]!!.jsonObject["x-f95gm-mutation"]!!.jsonPrimitive.content)
        assertContains(filters["post"]!!.jsonObject.keys, "requestBody")
        assertContains(filters["post"]!!.jsonObject["responses"]!!.jsonObject.keys, "400")
    }

    @Test
    fun operationsDescribeDifferentRequestResponseAndStatusShapes() {
        val contract = Json.parseToJsonElement(F95Api.openApiContractJson()).jsonObject
        val paths = contract["paths"]!!.jsonObject
        val login = paths["/api/login"]!!.jsonObject["post"]!!.jsonObject
        val health = paths["/api/health"]!!.jsonObject["get"]!!.jsonObject
        val media = paths["/api/media"]!!.jsonObject["get"]!!.jsonObject

        assertEquals(
            "LoginRequest",
            login["requestBody"]!!.jsonObject["content"]!!.jsonObject["application/json"]!!.jsonObject["schema"]!!.jsonObject["$" + "ref"]!!.jsonPrimitive.content.substringAfterLast(
                '/'
            )
        )
        assertEquals(
            setOf("200", "400", "401", "429", "500", "502", "503", "504"),
            login["responses"]!!.jsonObject.keys
        )
        assertEquals(setOf("200"), health["responses"]!!.jsonObject.keys)
        assertContains(media["responses"]!!.jsonObject["200"]!!.jsonObject["content"]!!.jsonObject.keys, "image/*")
        assertEquals("url", media["parameters"]!!.jsonArray.first().jsonObject["name"]!!.jsonPrimitive.content)
    }
}
