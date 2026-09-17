package f95gm.server.http.request

import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFails

class LimitedBodyTest {
    @Test
    fun acceptsTheBoundaryAndRejectsOneByteOverIt() {
        runTest {
            assertContentEquals("abc".encodeToByteArray(), ByteReadChannel("abc").readLimitedBytes(3))
            assertFails {
                ByteReadChannel("abcd").readLimitedBytes(3)
            }
        }
    }
}
