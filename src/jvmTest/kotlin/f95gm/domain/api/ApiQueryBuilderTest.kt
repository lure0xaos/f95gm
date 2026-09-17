package f95gm.domain.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ApiQueryBuilderTest {
    @Test
    fun buildsEmptyAndEncodedMultiParameterQueriesInOrder() {
        assertEquals("/api/catalog", apiQuery("/api/catalog").build().value)
        val builder = apiQuery("/api/catalog")
        assertSame(builder, builder.add(ApiQueryParameter("search"), "a b&c"))
        val result = builder
            .add(ApiQueryParameter("creator"), "M+S")
            .build()
        assertEquals("/api/catalog?search=a%20b%26c&creator=M%2BS", result.value)
    }

    @Test
    fun exposesCanonicalMessageKeySetAndApiPaths() {
        assertEquals("server.authenticationRequired", F95ApiMessageKeys.AUTHENTICATION_REQUIRED.value)
        assertEquals(
            setOf(
                F95ApiMessageKeys.AUTHENTICATION_REQUIRED,
                F95ApiMessageKeys.SESSION_EXPIRED,
                F95ApiMessageKeys.SIGN_IN_AGAIN,
                F95ApiMessageKeys.CREDENTIALS_REQUIRED,
                F95ApiMessageKeys.LOGIN_FAILED
            ),
            F95ApiMessageKeys.AUTHENTICATION
        )
        assertEquals("/api/marks", F95ApiPaths.MARKS)
        assertEquals("/api/filters", F95ApiPaths.FILTERS)
        assertEquals("/api/health", F95ApiPaths.HEALTH)
        assertEquals("/api/marks/17", F95ApiPaths.markPath(f95gm.domain.values.F95ThreadId("17")).value)
        assertEquals("/api/filters/favorites", F95ApiPaths.filterPath(f95gm.domain.values.F95FilterId("favorites")).value)
        assertEquals("/api/item/18", F95ApiPaths.itemPath(f95gm.domain.values.F95ThreadId("18")).value)
        assertEquals("/api/catalog/options?category=games", F95ApiPaths.catalogOptionsPath(f95gm.domain.filters.F95Category.GAMES).value)
        assertEquals(
            "/api/health?probe=2&tabId=tab_1&theme=dark",
            F95ApiPaths.healthPath(ApiProbeNumber(2), ApiBrowserTabId("tab_1"), ApiTheme("dark")).value
        )
        assertEquals("/api/shutdown?tabId=tab_1", F95ApiPaths.shutdownPath(ApiBrowserTabId("tab_1")).value)
    }

    @Test
    fun validatesTypedApiMetadataValuesIncludingCalendarBoundaries() {
        assertEquals("v2", ApiVersion("v2").value)
        assertEquals("2024-02-29", ApiSunsetDate("2024-02-29").value)
        assertEquals("2025-04-30", ApiSunsetDate("2025-04-30").value)
        assertEquals("2024-02-29", ApiDeprecationMetadata(ApiPath("/new"), ApiSunsetDate("2024-02-29")).sunset.value)
        assertFailsWith<IllegalArgumentException> { ApiVersion(" ") }
        listOf("2023-02-29", "2024-04-31", "2024-13-01", "24-01-01", "2024-00-01").forEach { date ->
            assertFailsWith<IllegalArgumentException>(date) { ApiSunsetDate(date) }
        }
        assertFailsWith<IllegalArgumentException> { ApiBrowserTabId("") }
        assertFailsWith<IllegalArgumentException> { ApiBrowserTabId("contains space") }
        assertFailsWith<IllegalArgumentException> { ApiBrowserTabId("x".repeat(129)) }
        assertFailsWith<IllegalArgumentException> { ApiProbeNumber(-1) }
        assertFailsWith<IllegalArgumentException> { ApiTheme("auto") }
    }
}
