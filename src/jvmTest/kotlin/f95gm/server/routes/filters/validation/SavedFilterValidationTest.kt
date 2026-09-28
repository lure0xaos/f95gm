package f95gm.server.routes.filters.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedFilterValidationTest {
    @Test
    fun acceptsCurrentIdsOnly() {
        assertTrue(isValidSavedFilterId("abc-123"))
        assertTrue(isValidSavedFilterId("42"))
        assertFalse(isValidSavedFilterId("00000000-0000-0000-0000-000000000001"))
    }

    @Test
    fun rejectsInvalidIds() {
        assertFalse(isValidSavedFilterId(""))
        assertFalse(isValidSavedFilterId("../../etc"))
        assertFalse(isValidSavedFilterId("a b"))
    }
}
