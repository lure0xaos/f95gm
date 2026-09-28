package f95gm.server.routes.marks.live

import f95gm.domain.values.F95ThreadId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MapCatalogInfoResponseTest {
    @Test
    fun mapsMatchingCatalogItemAndAppliesFallbacks() {
        val result = mapCatalogInfoResponse(
            """{"msg":{"data":[{"thread_id":42,"title":"","version":"","likes":"","views":""}]}}""",
            "Requested title",
            F95ThreadId("42")
        )

        val found = assertIs<CatalogInfoResult.Found>(result).value
        assertEquals("42", found.threadId.value)
        assertEquals("Requested title", found.title.value)
        assertEquals("0", found.likes.value)
        assertEquals("0", found.views.value)
    }

    @Test
    fun distinguishesMalformedPayloadFromMissingItem() {
        assertIs<CatalogInfoResult.InvalidPayload>(mapCatalogInfoResponse("{", "Title", F95ThreadId("42")))
        assertIs<CatalogInfoResult.NotFound>(
            mapCatalogInfoResponse("""{"msg":{"data":[]}}""", "Title", F95ThreadId("42"))
        )
    }
}
