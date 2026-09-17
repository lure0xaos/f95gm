package f95gm.server.kernel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LifecycleGateTest {
    @Test
    fun permitsOnlyOneTerminalTransition() {
        val gate = LifecycleGate()

        assertEquals(LifecycleState.OPEN, gate.currentState)
        assertTrue(gate.beginClose())
        assertFalse(gate.beginClose())
        assertEquals(LifecycleState.CLOSING, gate.currentState)
        assertTrue(gate.fail())
        assertFalse(gate.fail())
        assertFalse(gate.complete())
        assertEquals(LifecycleState.FAILED, gate.currentState)
    }
}
