package f95gm.server.http.request

import f95gm.domain.api.F95Api
import io.ktor.http.*
import kotlin.test.*

class ApiEndpointResolverTest {
    private val resolver = ApiEndpointResolver()

    @Test
    fun resolvesMethodsAndPathParametersAndIgnoresQueryStrings() {
        assertEquals(F95Api.ITEM_BY_THREAD, resolver.resolve("/api/item/42?tab=details", HttpMethod.Get)?.path)
        assertEquals(F95Api.MARK_BY_THREAD, resolver.resolve("/api/marks/abc", HttpMethod.Put)?.path)
        assertNull(resolver.resolve("/api/item/42", HttpMethod.Post))
        assertNull(resolver.resolve("/api/item", HttpMethod.Get))
    }

    @Test
    fun identifiesOnlyTheApiPathNamespace() {
        assertTrue(resolver.isApiPath("/api"))
        assertTrue(resolver.isApiPath("/api/catalog"))
        assertFalse(resolver.isApiPath("/apiary"))
        assertFalse(resolver.isApiPath("/catalog"))
    }
}
