package f95gm.server.state.runtime

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ServerContextLifecycleTest {
    @Test
    fun closeIsIdempotentAndPreventsNewUpstreamAccess() {
        val context = testServerContext()

        context.close()
        context.close()

        assertFailsWith<IllegalStateException> { context.upstream }
    }
}
