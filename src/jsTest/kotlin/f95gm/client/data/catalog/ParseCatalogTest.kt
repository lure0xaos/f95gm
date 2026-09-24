package f95gm.client.data.catalog

import f95gm.messages.UiMessages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParseCatalogTest {
    @Test
    fun mapsCatalogItemsAndNestedCoverData() {
        val result = parseCatalog(
            """
                {
                  "status": "ok",
                  "msg": {
                    "data": [{
                      "thread_id": "123",
                      "title": "Example",
                      "version": "1.0",
                      "creator": "Creator",
                      "cover": "",
                      "images": {"cover": "https://example.com/cover.jpg"},
                      "tags": ["1", null],
                      "prefixes": ["2"],
                      "date": "Today",
                      "likes": "10",
                      "views": "20",
                      "rating": 8.5,
                      "new": true
                    }],
                    "pagination": {"total": 3}
                  }
                }
            """.trimIndent()
        )

        val item = result.items.single()
        assertEquals("123", item.threadId.value)
        assertEquals("Example", item.title.value)
        assertEquals("Creator", item.developer.value)
        assertEquals("https://example.com/cover.jpg", item.cover.value)
        assertEquals(listOf("1"), item.tags.map { it.value })
        assertEquals(listOf("2"), item.prefixes.map { it.value })
        assertEquals(8.5, item.rating)
        assertEquals(true, item.isNew)
        assertEquals(3, result.totalPages)
    }

    @Test
    fun appliesFieldDefaultsAndClampsInvalidPagination() {
        val result = parseCatalog(
            """
                {"status":"ok","msg":{"data":[{"thread_id":"1","creator":"","developer":"Dev"}],"pagination":{"total":0}}}
            """.trimIndent()
        )

        val item = result.items.single()
        assertEquals(UiMessages.app_untitledRelease(), item.title.value)
        assertEquals(UiMessages.app_versionUnknown(), item.version.value)
        assertEquals("Dev", item.developer.value)
        assertEquals(0.0, item.rating)
        assertEquals(false, item.isNew)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun rejectsNonSuccessfulCatalogPayloads() {
        val error = assertFailsWith<IllegalStateException> {
            parseCatalog("{\"status\":\"error\",\"msg\":\"failed\"}")
        }

        assertEquals("failed", error.message)
    }
}
