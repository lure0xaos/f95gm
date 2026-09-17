package f95gm.server.routes.catalog

import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.test.Test
import kotlin.test.assertEquals
import f95gm.server.config.CatalogLimits
import f95gm.server.routes.ports.CatalogRequestPolicy

private val testCatalogRequestPolicy = CatalogRequestPolicy(
    defaultRows = CatalogLimits.PAGE_SIZE,
    allowedRows = CatalogLimits.PAGE_SIZES,
    maxPage = CatalogLimits.MAX_PAGE,
    maxTags = CatalogLimits.MAX_TAGS,
    maxTextLength = CatalogLimits.MAX_TEXT_LENGTH
)

class CatalogRequestTest {
    @Test
    fun appliesDefaultsAndSanitizesRepeatedValues() = testApplication {
        application {
            routing {
                get("/catalog") {
                    val request = call.catalogRequest(testCatalogRequestPolicy)
                    call.respondText(
                        listOf(
                            request.category,
                            request.page.value.toString(),
                            request.rows.toString(),
                            request.tags.joinToString(","),
                            request.excludedTags.joinToString(","),
                            request.search
                        ).joinToString("|")
                    )
                }
            }
        }

        val response = client.get(
            "/catalog?tags[]=12&tags[]=12&tags[]=bad&notags[]=7&search=%20hello%20"
        )

        assertEquals("${f95gm.domain.defaults.F95Defaults.DEFAULT_CATEGORY}|1|90|12|7|hello", response.bodyAsText())
    }

    @Test
    fun clampsInvalidScalarValuesToSafeDefaults() = testApplication {
        application {
            routing {
                get("/catalog") {
                    val request = call.catalogRequest(testCatalogRequestPolicy)
                    call.respondText("${request.page.value}|${request.rows}|${request.date}")
                }
            }
        }

        val response = client.get("/catalog?page=0&rows=999&date=999999")

        assertEquals("1|90|0", response.bodyAsText())
    }
}
