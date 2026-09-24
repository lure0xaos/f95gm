package f95gm.server.routes.item

import f95gm.server.state.runtime.SystemServerClock
import kotlin.test.Test
import kotlin.test.assertFalse

class PublicHostResolverTest {
    @Test
    fun freshResolutionRejectsLoopbackAddresses() {
        val resolver = PublicHostResolver(SystemServerClock)
        try {
            assertFalse(resolver.isFreshPublic("localhost"))
        } finally {
            resolver.close()
        }
    }
}
