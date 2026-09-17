package f95gm.server.state.runtime

import f95gm.server.kernel.CloseGuard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CloseGuardTest {
    @Test
    fun closesOnlyOnceAndExposesClosedState() {
        val guard = CloseGuard()
        var closes = 0

        guard.closeOnce { closes++ }
        guard.closeOnce { closes++ }

        assertEquals(1, closes)
        assertTrue(guard.isClosed)
    }

    @Test
    fun failedCloseIsNotRetriedAndPreservesTheFirstFailure() {
        val guard = CloseGuard()
        val failure = IllegalStateException("close failed")
        var closes = 0

        assertFailsWith<IllegalStateException> {
            guard.closeOnce {
                closes++
                throw failure
            }
        }
        guard.closeOnce { closes++ }

        assertEquals(1, closes)
        assertTrue(guard.isClosed)
    }
}
