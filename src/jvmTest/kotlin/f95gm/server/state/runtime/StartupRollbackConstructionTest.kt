package f95gm.server.state.runtime

import f95gm.server.kernel.LifecycleResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class StartupRollbackConstructionTest {
    @Test
    fun failedConstructionClosesRegisteredResourcesInReverseOrderAndKeepsTheCause() {
        val rollback = StartupRollbackStack()
        val closed = mutableListOf<String>()
        val failure = IllegalArgumentException("construction failed")
        val thrown = assertFailsWith<IllegalArgumentException> {
            rollback.construct { stack ->
                stack.register("first", resource("first", closed))
                stack.register("second", resource("second", closed))
                throw failure
            }
        }

        assertSame(failure, thrown)
        assertEquals(listOf("second", "first"), closed)
        assertEquals(0, rollback.rollback().size)
    }

    @Test
    fun successfulConstructionDisarmsItsTemporaryRollbackStack() {
        val rollback = StartupRollbackStack()
        val closed = mutableListOf<String>()

        val result = rollback.construct { stack ->
            stack.register("owned-by-runtime", resource("owned-by-runtime", closed))
            "runtime"
        }

        assertEquals("runtime", result)
        assertTrue(rollback.rollback().isEmpty())
        assertTrue(closed.isEmpty())
    }

    private fun resource(name: String, closed: MutableList<String>): LifecycleResource = object : LifecycleResource {
        override val isClosed: Boolean = false
        override fun close() {
            closed += name
        }
    }
}
