package f95gm.server.state.runtime.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EvictionPolicyTest {
    @Test
    fun leastRecentlyUsedSelectsTheEntryWithTheOldestAccessTime() {
        val entries = mapOf("new" to 200L, "old" to 10L, "middle" to 100L).entries

        assertEquals("old", leastRecentlyUsed<String, Long> { it }.select(entries))
    }

    @Test
    fun leastRecentlyUsedReturnsNullForAnEmptyCollection() {
        assertNull(leastRecentlyUsed<String, Long> { it }.select(emptyList()))
    }
}
