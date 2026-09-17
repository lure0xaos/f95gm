package f95gm.server.state.runtime.session

import f95gm.domain.values.F95SessionId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SessionLockRegistryTest {
    @Test
    fun serializesOperationsForTheSameSession() = runTest {
        val registry = SessionLockRegistry()
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000030")
        var active = 0
        var maximum = 0

        listOf(1, 2).map {
            async {
                registry.withLock(sessionId) {
                    active++
                    maximum = maxOf(maximum, active)
                    delay(10)
                    active--
                }
            }
        }.awaitAll()

        assertEquals(1, maximum)
    }

    @Test
    fun rejectsNewLocksAfterClose() = runTest {
        val registry = SessionLockRegistry()
        registry.close()

        assertFailsWith<IllegalStateException> {
            registry.withLock(F95SessionId("00000000-0000-0000-0000-000000000031")) { }
        }
        assertEquals(true, registry.isClosed)
    }
}
