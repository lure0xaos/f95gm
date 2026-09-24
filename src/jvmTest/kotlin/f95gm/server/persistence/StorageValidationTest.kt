package f95gm.server.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StorageValidationTest {
    @Test
    fun acceptsValuesAtTheStorageBoundaryAndReturnsTheOriginalValue() {
        val value = "abc"

        assertEquals(value, requireStorageLength(value, 3, "name"))
        assertEquals("", requireStorageLength("", 0, "name"))
    }

    @Test
    fun rejectsValuesThatExceedTheStorageBoundaryWithFieldContext() {
        val error = assertFailsWith<IllegalArgumentException> {
            requireStorageLength("abcd", 3, "name")
        }

        assertEquals("name exceeds the database limit of 3 characters", error.message)
    }
}
