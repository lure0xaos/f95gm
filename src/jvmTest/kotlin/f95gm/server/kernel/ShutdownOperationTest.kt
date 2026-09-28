package f95gm.server.kernel

import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ShutdownOperationTest {
    @Test
    fun preservesTheFirstFailureAcrossRepeatedTerminalFailureAttempts() {
        val operation = ShutdownOperation()
        val first = IllegalStateException("first")
        val second = IllegalArgumentException("second")

        assertSame(first, assertFailsWith<IllegalStateException> { operation.fail(first) })
        assertSame(first, assertFailsWith<IllegalStateException> { operation.fail(second) })

        assertTrue(operation.phase == ShutdownPhase.FAILED)
        assertSame(first, operation.failure)
    }
}
