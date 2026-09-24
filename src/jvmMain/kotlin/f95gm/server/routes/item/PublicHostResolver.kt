package f95gm.server.routes.item

import f95gm.server.state.runtime.CloseGuard
import f95gm.server.state.runtime.LifecycleResource
import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.cache.BoundedTtlCache
import java.net.InetAddress
import java.net.UnknownHostException

internal class PublicHostResolver(private val clock: ServerClock) : LifecycleResource {
    private val cache = BoundedTtlCache<String, Boolean>(256, 300_000L, clock)
    private val closeGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed

    fun isPublic(host: String): Boolean {
        check(!closeGuard.isClosed) { "Public host resolver is closed" }
        return cache.getOrPut(host.lowercase()) {
            resolveAddresses(host).let { addresses ->
                addresses.isNotEmpty() && addresses.all { address ->
                    !address.isAnyLocalAddress &&
                            !address.isLoopbackAddress &&
                            !address.isLinkLocalAddress &&
                            !address.isSiteLocalAddress &&
                            !address.isMulticastAddress
                }
            }
        }
    }

    /** Performs a fresh DNS lookup for security-sensitive proxy requests. */
    fun isFreshPublic(host: String): Boolean {
        return freshPublicAddresses(host).isNotEmpty()
    }

    /** Resolves once and returns only addresses safe for an outbound proxy connection. */
    fun freshPublicAddresses(host: String): List<InetAddress> {
        check(!closeGuard.isClosed) { "Public host resolver is closed" }
        val addresses = resolveAddresses(host)
        return addresses.filter { address ->
            !address.isAnyLocalAddress &&
                    !address.isLoopbackAddress &&
                    !address.isLinkLocalAddress &&
                    !address.isSiteLocalAddress &&
                    !address.isMulticastAddress
        }
    }

    override fun close() {
        closeGuard.closeOnce { cache.clear() }
    }

    private fun resolveAddresses(host: String): Array<InetAddress> = try {
        InetAddress.getAllByName(host)
    } catch (_: UnknownHostException) {
        emptyArray()
    }
}
