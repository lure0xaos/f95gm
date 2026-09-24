package f95gm.server.state.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
}
