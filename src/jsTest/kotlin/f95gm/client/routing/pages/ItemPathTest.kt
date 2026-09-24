package f95gm.client.routing.pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ItemPathTest {
    @Test
    fun extractsNumericThreadIdsWithOptionalTrailingSlash() {
        assertEquals("12345", itemThreadId("/item/12345"))
        assertEquals("12345", itemThreadId("/item/12345/"))
    }

    @Test
    fun rejectsNonItemNonNumericAndEmptyPaths() {
        listOf("", "/", "/catalog", "/item/", "/item/12abc", "/item/12/extra").forEach {
            assertNull(itemThreadId(it))
        }
    }
}
