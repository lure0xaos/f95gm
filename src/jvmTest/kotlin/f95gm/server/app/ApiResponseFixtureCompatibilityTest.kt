package f95gm.server.app

import f95gm.domain.api.RequestMetricsContract
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiResponseFixtureCompatibilityTest {
    @Test
    fun storedMetricsResponseFixtureRemainsDeserializable() {
        val fixture = requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/metrics-response.json"))
            .bufferedReader().use { it.readText() }
        val response = Json.decodeFromString<RequestMetricsContract>(fixture)
        assertEquals(12, response.requests)
        assertEquals(8, response.cacheNamespaces.getValue("live-mark").hits)
    }

    @Test
    fun additiveUnknownFieldsAreIgnoredByCompatibilityReader() {
        val response = Json { ignoreUnknownKeys = true }
            .decodeFromString<RequestMetricsContract>("""{"requests":1,"rejected":0,"retries":0,"evictions":0,"circuitOpenings":0,"cacheNamespaces":{},"futureField":"ignored"}""")
        assertEquals(1, response.requests)
    }

    @Test
    fun omittedNewerOptionalFieldsKeepTheirCompatibilityDefaults() {
        val response = Json { ignoreUnknownKeys = true }
            .decodeFromString<RequestMetricsContract>("""{"requests":2,"rejected":0,"retries":0,"evictions":0,"circuitOpenings":0}""")
        assertTrue(response.cacheNamespaces.isEmpty())
    }
}
