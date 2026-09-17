package f95gm.server.http.request

import f95gm.domain.api.ApiQueryParameter
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TypedQueryParametersTest {
    @Test
    fun appliesTrimmingLengthBoundsAllowedValuesAndDefaults() = testApplication {
        application {
            routing {
                get("/params") {
                    val values = listOf(
                        call.request.textParameter(ApiQueryParameter("text"), 5),
                        call.request.allowedTextParameter(ApiQueryParameter("sort"), "default", listOf("new", "old")),
                        call.request.oneOfIntParameter(ApiQueryParameter("rows"), 10, listOf(10, 20)),
                        call.request.intParameter(ApiQueryParameter("page"), 1, 1..50),
                        call.request.enumParameter(ApiQueryParameter("mode"), Mode.DEFAULT, Mode.entries, Mode::wire),
                        call.request.requiredTextParameter(ApiQueryParameter("required"), 10).orEmpty()
                    )
                    call.respondText(values.joinToString("|"))
                }
            }
        }

        val response = client.get("/params?text= 123456 &sort=invalid&rows=15&page=0&mode=compact&required=%20")

        assertEquals("12345|default|10|1|COMPACT|", response.bodyAsText())
    }

    @Test
    fun readsRepeatedCanonicalAndBracketedValuesThroughOneTypedBoundary() = testApplication {
        application {
            routing {
                get("/values") {
                    call.respondText(call.request.valuesParameter(f95gm.domain.api.F95ApiQueryParameters.TAGS).joinToString(","))
                }
            }
        }

        assertEquals("one,three,two", client.get("/values?tags=one&tags[]=two&tags=three").bodyAsText())
    }

    private enum class Mode(val wire: String) {
        DEFAULT("default"),
        COMPACT("compact")
    }
}
