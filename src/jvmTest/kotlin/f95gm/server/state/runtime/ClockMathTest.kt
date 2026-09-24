package f95gm.server.state.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClockMathTest {
    @Test
    fun elapsedChecksDoNotTreatClockRollbackAsElapsed() {
        assertFalse(elapsedAtLeast(99, 100, 1))
        assertTrue(elapsedAtLeast(101, 100, 1))
    }

    @Test
    fun arithmeticSaturatesAtLongMaximum() {
        assertEquals(Long.MAX_VALUE, saturatingAdd(Long.MAX_VALUE - 1, 10))
        assertEquals(Long.MAX_VALUE, saturatingMultiply(Long.MAX_VALUE, 2))
        assertEquals(Long.MAX_VALUE, saturatingDouble(Long.MAX_VALUE))
        assertEquals(0L, saturatingMultiply(0, Long.MAX_VALUE))
        assertEquals(0L, saturatingDouble(0))
    }
}
