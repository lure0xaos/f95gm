package f95gm.server.routes.filters.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedFilterValidationTest {
    @Test
    fun acceptsCurrentAndLegacyIds() {
        assertTrue(isValidSavedFilterId("abc-123"))
        assertTrue(isValidSavedFilterId("42"))
    }

    @Test
    fun rejectsInvalidIds() {
        assertFalse(isValidSavedFilterId(""))
        assertFalse(isValidSavedFilterId("../../etc"))
        assertFalse(isValidSavedFilterId("a b"))
    }
}
