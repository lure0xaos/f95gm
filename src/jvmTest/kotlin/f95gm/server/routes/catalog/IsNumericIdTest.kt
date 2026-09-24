package f95gm.server.routes.catalog

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsNumericIdTest {
    @Test
    fun acceptsOnlyPositiveBaseTenIntegers() {
        assertTrue(isNumericId("1"))
        assertTrue(isNumericId("42"))
        assertFalse(isNumericId("0"))
        assertFalse(isNumericId("-1"))
        assertFalse(isNumericId("1.5"))
        assertFalse(isNumericId(" 42 "))
        assertFalse(isNumericId(""))
    }
}
