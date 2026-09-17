package f95gm.server.upstream.media

import f95gm.server.application.media.HostName
import f95gm.server.application.media.PublicHostResolverPort
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource
import java.net.InetAddress
import java.net.UnknownHostException
import f95gm.server.kernel.ServerInvariantViolation

internal interface PublicHostCache {
    fun getOrPut(host: String, factory: () -> Boolean): Boolean
    fun clear()
}

internal class PublicHostResolver(private val cache: PublicHostCache) : LifecycleResource, PublicHostResolverPort {
    private val closeGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed

    fun isPublic(host: HostName): Boolean {
        if (closeGuard.isClosed) throw ServerInvariantViolation("Public host resolver is closed")
        return cache.getOrPut(host.value.lowercase()) {
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
    override fun isFreshPublic(host: HostName): Boolean {
        return freshPublicAddresses(host).isNotEmpty()
    }

    /** Resolves once and returns only addresses safe for an outbound proxy connection. */
    override fun freshPublicAddresses(host: HostName): List<InetAddress> {
        if (closeGuard.isClosed) throw ServerInvariantViolation("Public host resolver is closed")
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

    private fun resolveAddresses(host: HostName): Array<InetAddress> = try {
        InetAddress.getAllByName(host.value)
    } catch (_: UnknownHostException) {
        emptyArray()
    }
}
