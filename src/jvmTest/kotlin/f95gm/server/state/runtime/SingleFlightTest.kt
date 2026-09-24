package f95gm.server.state.runtime

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SingleFlightTest {
    @Test
    fun concurrentCallersShareOneRead() = runTest {
        val flight = SingleFlight<String, Int>(CoroutineScope(Dispatchers.Default))
        var loads = 0
        val values = (1..8).map {
            async {
                flight.run("same") {
                    loads++
                    delay(10)
                    42
                }
            }
        }.awaitAll()
        assertEquals(List(8) { 42 }, values)
        assertEquals(1, loads)
    }

    @Test
    fun rejectsWorkAfterClose() {
        runTest {
            val flight = SingleFlight<String, Int>(CoroutineScope(Dispatchers.Default))
            flight.close()
            assertFailsWith<IllegalStateException> {
                flight.run("closed") { 1 }
            }
        }
    }
}
