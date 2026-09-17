package f95gm.server.upstream.media

import f95gm.server.application.media.HostName
import kotlin.test.Test
import kotlin.test.assertFalse

class PublicHostResolverTest {
    @Test
    fun freshResolutionRejectsLoopbackAddresses() {
        val resolver = PublicHostResolver(object : PublicHostCache {
            override fun getOrPut(host: String, factory: () -> Boolean): Boolean = factory()
            override fun clear() = Unit
        })
        try {
            assertFalse(resolver.isFreshPublic(HostName("localhost")))
        } finally {
            resolver.close()
        }
    }
}
