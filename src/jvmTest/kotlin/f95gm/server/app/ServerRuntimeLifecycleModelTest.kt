package f95gm.server.app

import f95gm.server.kernel.ServerInvariantViolation
import f95gm.server.desktop.DesktopLifecycle
import f95gm.server.desktop.DesktopTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ServerRuntimeLifecycleModelTest {
    @Test
    fun lifecycleSlotBindsReplacesAndCanBeTakenOnlyOnce() {
        val slot = LifecycleSlot<String>()
        assertNull(slot.peek())
        assertFailsWith<ServerInvariantViolation> { slot.get() }

        slot.bind("first")
        assertEquals("first", slot.get())
        slot.replace("second")
        assertEquals("second", slot.peek())
        assertEquals("second", slot.take())
        assertNull(slot.peek())
        assertNull(slot.take())
        assertFailsWith<ServerInvariantViolation> { slot.get() }
        assertFailsWith<IllegalStateException> { slot.bind("after-take") }
    }

    @Test
    fun initializedSlotCanBeReplacedButCannotBeBoundAgain() {
        val slot = LifecycleSlot("initial")
        assertEquals("initial", slot.get())
        assertFailsWith<IllegalStateException> { slot.bind("duplicate") }
        slot.replace("replacement")
        assertEquals("replacement", slot.get())
    }

    @Test
    fun lifecycleStatesMapToTheirPublicPhases() {
        assertEquals(ServerRuntimePhase.NEW, RuntimeLifecycleState.NEW.toPublicPhase())
        assertEquals(ServerRuntimePhase.SERVER_BOUND, RuntimeLifecycleState.SERVER_BOUND.toPublicPhase())
        assertEquals(ServerRuntimePhase.RUNNING, RuntimeLifecycleState.RUNNING.toPublicPhase())
        assertEquals(ServerRuntimePhase.CLOSED, RuntimeLifecycleState.CLOSED.toPublicPhase())
    }

    @Test
    fun shutdownPlanContinuesCleanupAndReportsDesktopFailure() {
        var disposed = false
        val desktop = object : DesktopLifecycle {
            override fun start() = Unit
            override fun updateTheme(theme: DesktopTheme?) = Unit
            override fun dispose() { disposed = true; error("dispose failed") }
        }

        val failure = assertFailsWith<ServerInvariantViolation> {
            RuntimeShutdownPlan.close(desktop, null, null)
        }

        assertEquals(true, disposed)
        assertEquals("Failed to close one or more server runtime resources", failure.message)
        assertEquals("dispose failed", failure.cause?.message)
    }
}
