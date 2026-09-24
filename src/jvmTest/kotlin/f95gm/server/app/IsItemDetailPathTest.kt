package f95gm.server.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsItemDetailPathTest {
    @Test
    fun recognizesNumericItemPathsWithOptionalTrailingSlash() {
        assertTrue(isItemDetailPath("item/123"))
        assertTrue(isItemDetailPath("item/123/"))
        assertTrue(isItemDetailPath("item/0"))
    }

    @Test
    fun rejectsEmptyNonNumericAndWronglyPrefixedPaths() {
        listOf("item/", "item/12x", "/item/12", "items/12", "catalog/12").forEach {
            assertFalse(isItemDetailPath(it))
        }
    }
}
