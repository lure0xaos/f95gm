package f95gm.server.state.runtime

import f95gm.server.kernel.BoundedCardinality

import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertTrue

class BoundedCardinalityTest {
    @Test
    fun concurrentAdmissionNeverExceedsTheConfiguredLimit() {
        val tracker = BoundedCardinality<Int>(8)
        val executor = Executors.newFixedThreadPool(16)
        try {
            val admitted = (0 until 1_000).map { value ->
                executor.submit<Boolean> { tracker.admit(value) }
            }.map { it.get() }

            assertTrue(admitted.count { it } <= 8)
            assertTrue(tracker.size <= 8)
        } finally {
            executor.shutdownNow()
        }
    }
}
